package com.jjg.game.season.service;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.sampledata.bean.SeasonMatchCfg;
import com.jjg.game.sampledata.bean.SeasonSimulationDataCfg;
import com.jjg.game.sampledata.bean.SeasonStartCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.season.dao.SeasonPlayerDao;
import com.jjg.game.season.data.SeasonMatchRecord;
import com.jjg.game.season.data.SeasonMatchResult;
import com.jjg.game.season.data.SeasonMatchSession;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.model.SeasonPhase;
import com.jjg.game.sim.service.SimAutoSaveService;
import com.jjg.game.sim.service.SimConfigCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 赛季异步匹配和基于 slots 上报结果的结算服务。
 */
@Service
public class SeasonMatchService {
    private static final Logger log = LoggerFactory.getLogger(SeasonMatchService.class);
    private static final int ADVANCED_SPIN_COUNT = 3;
    private static final int LOOP_SPIN_COUNT = 5;
    private static final int ACTIVE_MATCH_ITEM_ID = 1130001;
    private static final long ACTIVE_MATCH_ITEM_COUNT = 1L;
    /**
     * 赛季入口 (ReqChooseWare.enterType=1): 只有该入口的旋转下注赛季币, 才计入对局与代表战绩。
     */
    private static final int ENTER_TYPE_SEASON = 1;
    private static final int CANDIDATE_LIMIT = 20;
    private static final int HISTORY_LIMIT = 50;
    /**
     * 对局超时: 超时后按弃赛处理, 未完成的局按 0 收益补齐结算 (弃赛判负, 防止先看牌不利再放弃重开)。
     */
    private static final long MATCH_TIMEOUT_MILLIS = 30L * 60 * 1000;
    /**
     * 循环赛季对局中掉线自动补完阈值: 掉线超过该时长后再次进入赛季,
     * 剩余局按赛季模拟数据自动补完并结算。
     */
    private static final long OFFLINE_AUTO_PLAY_MILLIS = 60L * 1000;

    private final SeasonConfigService configService;
    private final SeasonPlayerDao seasonPlayerDao;
    private final SeasonEconomyService economyService;
    private final SimAutoSaveService autoSaveService;
    private final RobotUtil robotUtil;
    private final SimConfigCacheService simConfigCacheService;
    private final PlayerPackService playerPackService;

    public SeasonMatchService(SeasonConfigService configService, SeasonPlayerDao seasonPlayerDao,
                              SeasonEconomyService economyService, SimAutoSaveService autoSaveService,
                              RobotUtil robotUtil, SimConfigCacheService simConfigCacheService,
                              PlayerPackService playerPackService) {
        this.configService = configService;
        this.seasonPlayerDao = seasonPlayerDao;
        this.economyService = economyService;
        this.autoSaveService = autoSaveService;
        this.robotUtil = robotUtil;
        this.simConfigCacheService = simConfigCacheService;
        this.playerPackService = playerPackService;
    }

    public CommonResult<SeasonMatchSession> start(SimPlayerContext ctx, int gameType, long stake, long now) {
        return start(ctx, gameType, stake, now, true);
    }

    public CommonResult<SeasonMatchSession> startPassive(SimPlayerContext ctx, int gameType, long stake, long now) {
        return start(ctx, gameType, stake, now, false);
    }

    private CommonResult<SeasonMatchSession> start(SimPlayerContext ctx, int gameType, long stake, long now,
                                                   boolean activeMatch) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (data == null || data.seasonPhase() == null || data.seasonPhase() == SeasonPhase.NOVICE) {
            return failStart(Code.NOT_UNLOCKED, ctx, gameType, stake, "阶段未开放");
        }
        settleIfExpired(ctx, now);
        if (data.getActiveMatch() != null) {
            return failStart(Code.REPEAT_OP, ctx, gameType, stake, "已有进行中的对局");
        }
        SeasonStartCfg season = configService.season(data.getSeasonId());
        if (season == null || season.getAvailableGames() != gameType) {
            return failStart(Code.PARAM_ERROR, ctx, gameType, stake, "游戏不属于当前赛季");
        }
        SeasonMatchCfg cfg = configService.matchForDay(currentDay(data, now));
        if (cfg == null) {
            return failStart(Code.NOT_FOUND, ctx, gameType, stake, "匹配配置不存在");
        }
        if (stake <= 0 || stake > cfg.getMaxBet()) {
            return failStart(Code.BET_TO_LIMIT, ctx, gameType, stake, "下注额超限");
        }
        if (cfg.getDailyMatchLimit() >= 0 && data.getDailyMatchCount() >= cfg.getDailyMatchLimit()) {
            return failStart(Code.BET_TO_LIMIT, ctx, gameType, stake, "每日次数已满");
        }
        if (data.getLastMatchTime() > 0 && now - data.getLastMatchTime() < cfg.getMatchCD() * 1000L) {
            return failStart(Code.REPEAT_OP, ctx, gameType, stake, "匹配冷却中");
        }
        if (data.getDailyWinAmount() >= cfg.getDailyWinLimit()
                && data.getDailyPostLimitBet() >= cfg.getBetAmount()) {
            return failStart(Code.BET_TO_LIMIT, ctx, gameType, stake, "超出赢取上限后的下注量");
        }
        int expectedSpins = data.seasonPhase() == SeasonPhase.ADVANCED
                ? ADVANCED_SPIN_COUNT : LOOP_SPIN_COUNT;
        long requiredCoin = activeMatch ? stake : stake * expectedSpins;
        if (data.getSeasonCoin() < requiredCoin) {
            return failStart(Code.NOT_ENOUGH, ctx, gameType, stake,
                    "赛季币不足,required=" + requiredCoin);
        }
        List<SeasonPlayerData> candidates = seasonPlayerDao.findMatchCandidates(
                data.getSeasonKey(), ctx.playerId(), gameType, stake, expectedSpins, CANDIDATE_LIMIT);

        SeasonPlayerData opponent = pickOpponent(candidates, cfg, expectedSpins);
        if (opponent == null) {
            // 无人可匹配: 仅用机器人模拟展示数据, 不落库、不进匹配池
            opponent = createRobotOpponent(data.seasonPhase().ordinal() + 1, stake, data.getTierId());
            if (opponent == null) {
                return failStart(Code.NOT_FOUND, ctx, gameType, stake, "没有可用对手且机器人配置为空");
            }
            log.info("赛季匹配使用机器人对手 playerId={},robotId={},gameType={},stake={}",
                    ctx.playerId(), opponent.getPlayerId(), gameType, stake);
        }

        SeasonMatchSession session = new SeasonMatchSession();
        session.setMatchId(RandomUtils.getOriginalUUid());
        session.setOpponentId(opponent.getPlayerId());
        session.setOpponentName(opponent.getPlayerName());
        session.setOpponentHeadImgId(opponent.getHeadImgId());
        session.setOpponentHeadFrameId(opponent.getHeadFrameId());
        session.setOpponentTierId(opponent.getTierId());
        session.setGameType(gameType);
        session.setStake(stake);
        session.setStakeEscrowed(activeMatch);
        session.setExpectedSpins(expectedSpins);
        session.setStartedAt(now);

        if (opponent.getRepresentativeSpinWins().size() <= expectedSpins) {
            session.setOpponentSpinWins(opponent.getRepresentativeSpinWins());
        } else {
            session.setOpponentSpinWins(opponent.getRepresentativeSpinWins().subList(0, expectedSpins));
        }

        if (activeMatch) {
            CommonResult<?> itemResult = playerPackService.removeItems(ctx.getPlayer(),
                    Map.of(ACTIVE_MATCH_ITEM_ID, ACTIVE_MATCH_ITEM_COUNT), AddType.USE_ITEM,
                    "season-active-match");
            if (!itemResult.success()) {
                return failStart(itemResult.code, ctx, gameType, stake, "主动匹配道具不足");
            }
        }
        if (session.isStakeEscrowed()) {
            data.setSeasonCoin(data.getSeasonCoin() - stake);
        }
        data.setActiveMatch(session);
        data.setLastMatchTime(now);
        data.setDailyMatchCount(data.getDailyMatchCount() + 1);
        if (data.getDailyWinAmount() >= cfg.getDailyWinLimit()) {
            data.setDailyPostLimitBet(data.getDailyPostLimitBet() + stake);
        }
        autoSaveService.enqueueSave(data);
        return new CommonResult<>(Code.SUCCESS, session);
    }

    /**
     * 从候选中加权抽取真实对手; 池为空时返回 null, 由调用方回退机器人。
     */
    private SeasonPlayerData pickOpponent(List<SeasonPlayerData> candidates, SeasonMatchCfg cfg, int expectedSpins) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        //查询已按 minSpins 过滤, 这里兜底复核一次
        WeightRandom<SeasonPlayerData> candidatePool = WeightRandom.create();
        for (SeasonPlayerData candidate : candidates) {
            if (candidate.getRepresentativeSpinWins().size() < expectedSpins) {
                continue;
            }
            int weight = matchWeight(candidate, cfg, true);
            if (weight > 0) {
                candidatePool.add(candidate, weight);
            }
        }
        return candidatePool.next();
    }

    private SeasonPlayerData createRobotOpponent(int phase, long stake, int tierId) {
        RobotPlayer robot = robotUtil.randomRobotPlayer();
        if (robot == null) {
            return null;
        }
        SeasonPlayerData opponent = new SeasonPlayerData();
        opponent.setPlayerId(robot.getId());
        opponent.setPlayerName(robot.getNickName());
        opponent.setHeadImgId(robot.getHeadImgId());
        opponent.setHeadFrameId(robot.getHeadFrameId());
        opponent.setTierId(tierId);

        List<Integer> multipliers = pickSimulationMultipliers(phase);
        if (multipliers != null) {
            List<Long> representativeSpinWins = new ArrayList<>();
            for (int num : multipliers) {
                representativeSpinWins.add(stake * num);
            }
            opponent.setRepresentativeSpinWins(representativeSpinWins);
        }
        return opponent;
    }

    /**
     * 从赛季模拟数据按权重抽取一组倍率 (机器人对手/掉线自动补完共用); 无可用配置时返回 null。
     */
    private List<Integer> pickSimulationMultipliers(int phase) {
        List<SeasonSimulationDataCfg> cfgs = simConfigCacheService.getSeasonSimulationDataCfgMap().get(phase);
        if (cfgs == null || cfgs.isEmpty()) {
            return null;
        }
        WeightRandom<SeasonSimulationDataCfg> candidatePool = WeightRandom.create();
        for (SeasonSimulationDataCfg cfg : cfgs) {
            if (cfg.getExtractionWeight() > 0) {
                candidatePool.add(cfg, cfg.getExtractionWeight());
            }
        }
        SeasonSimulationDataCfg next = candidatePool.next();
        return next == null ? null : next.getMultiplier();
    }

    /**
     * 热路径: 无对局、非赛季入口、非本局游戏或无统计信息时静默跳过 (玩家可能同时在玩其他机台)。
     */
    public CommonResult<SeasonMatchResult> onSpin(SimPlayerContext ctx, int gameType, SpinStatInfo statInfo,
                                                  int enterType, long now) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonMatchSession session = data == null ? null : data.getActiveMatch();
        if (session == null) {
            seedRepresentativeIfAbsent(ctx, gameType, statInfo, enterType);
            return new CommonResult<>(Code.SUCCESS);
        }
        if (now - session.getStartedAt() >= MATCH_TIMEOUT_MILLIS) {
            //超时弃赛: 先补 0 结算旧局, 本次旋转不计入
            return settleExpired(ctx, session, now);
        }
        //对局只认赛季入口的赛季币旋转: 普通入口的金币局即使机台与下注数值相同也不计入
        if (enterType != ENTER_TYPE_SEASON || session.getGameType() != gameType
                || statInfo == null || statInfo.getBet() != session.getStake()) {
            return new CommonResult<>(Code.SUCCESS);
        }
        if (session.getPlayerSpinWins().size() < session.getExpectedSpins()) {
            session.getPlayerSpinWins().add(Math.max(0, statInfo.getWin()));
            //掉线后回来继续旋转: 视为回归, 清除掉线标记
            data.setMatchOfflineTime(0);
        }
        if (session.getPlayerSpinWins().size() < session.getExpectedSpins()) {
            return new CommonResult<>(Code.SUCCESS);
        }
        return settle(ctx, session, now);
    }

    /**
     * 进行中的对局超时则按弃赛补 0 结算; 匹配入口与玩家 tick 调用,
     * 保证玩家不会被残留对局锁住押金/卡死匹配。
     *
     * @return 发生结算时返回结算结果, 否则 null
     */
    public SeasonMatchResult settleIfExpired(SimPlayerContext ctx, long now) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonMatchSession session = data == null ? null : data.getActiveMatch();
        if (session != null && now - session.getStartedAt() >= MATCH_TIMEOUT_MILLIS) {
            return settleExpired(ctx, session, now).data;
        }
        return null;
    }

    /**
     * 进入赛季入口调用: 循环赛季对局中掉线超过 {@link #OFFLINE_AUTO_PLAY_MILLIS} 的,
     * 剩余局按赛季模拟数据自动补完并结算; 未超阈值视为回归, 清除掉线标记后对局继续。
     *
     * @return 发生结算时返回结算结果, 否则 null
     */
    public SeasonMatchResult settleOfflineMatch(SimPlayerContext ctx, long now) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonMatchSession session = data == null ? null : data.getActiveMatch();
        if (session == null || data.getMatchOfflineTime() <= 0) {
            return null;
        }
        if (!offlineAutoPlayDue(data, now)) {
            data.setMatchOfflineTime(0);
            autoSaveService.enqueueSave(data);
            return null;
        }
        return settleOfflineAutoPlay(ctx, session, now).data;
    }

    private boolean offlineAutoPlayDue(SeasonPlayerData data, long now) {
        return data.getMatchOfflineTime() > 0
                && now - data.getMatchOfflineTime() >= OFFLINE_AUTO_PLAY_MILLIS;
    }

    private CommonResult<SeasonMatchResult> settleExpired(SimPlayerContext ctx, SeasonMatchSession session, long now) {
        //掉线中的对局按自动补完结算, 仅在线弃赛才按 0 收益判负
        if (offlineAutoPlayDue(ctx.getSeasonPlayerData(), now)) {
            return settleOfflineAutoPlay(ctx, session, now);
        }
        log.info("赛季对局超时弃赛结算 playerId={},matchId={},recorded={}",
                ctx.playerId(), session.getMatchId(), session.getPlayerSpinWins().size());
        while (session.getPlayerSpinWins().size() < session.getExpectedSpins()) {
            session.getPlayerSpinWins().add(0L);
        }
        return settle(ctx, session, now);
    }

    private CommonResult<SeasonMatchResult> settleOfflineAutoPlay(SimPlayerContext ctx,
                                                                  SeasonMatchSession session, long now) {
        log.info("赛季对局掉线自动补完结算 playerId={},matchId={},recorded={}",
                ctx.playerId(), session.getMatchId(), session.getPlayerSpinWins().size());
        List<Integer> multipliers = pickSimulationMultipliers(
                ctx.getSeasonPlayerData().seasonPhase().ordinal() + 1);
        List<Long> wins = session.getPlayerSpinWins();
        while (wins.size() < session.getExpectedSpins()) {
            int index = wins.size();
            wins.add(multipliers != null && index < multipliers.size()
                    ? session.getStake() * multipliers.get(index) : 0L);
        }
        return settle(ctx, session, now);
    }

    private CommonResult<SeasonMatchResult> settle(SimPlayerContext ctx, SeasonMatchSession session, long now) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        data.setMatchOfflineTime(0);
        if (!data.markMatchProcessed(session.getMatchId(), HISTORY_LIMIT)) {
            //同一 matchId 已结算过 (重试/掉线补完与超时弃赛撞车), 丢弃本次避免重复改币
            log.warn("赛季对局重复结算, 已跳过 playerId={},matchId={}", ctx.playerId(), session.getMatchId());
            data.setActiveMatch(null);
            return new CommonResult<>(Code.REPEAT_OP);
        }
        long playerTotal = sum(session.getPlayerSpinWins());
        long opponentTotal = sum(session.getOpponentSpinWins());
        long rawChange = playerTotal - opponentTotal;
        // 主动匹配先退还托管押注，再统一按分差结算；被动匹配未预扣，无需返还。
        if (session.isStakeEscrowed()) {
            data.setSeasonCoin(data.getSeasonCoin() + session.getStake());
        }
        long actualChange = rawChange;
        SeasonMatchCfg cfg = configService.matchForDay(currentDay(data, now));
        if (rawChange > 0) {
            if (cfg != null && data.getDailyWinAmount() >= cfg.getDailyWinLimit()) {
                actualChange = SeasonPolicy.applyRatio(rawChange,
                        SeasonPolicy.ratioFor(data.getDailyWinAmount() - cfg.getDailyWinLimit(), cfg.getProfitRatio()));
            }
            economyService.addEarnedCoin(ctx, actualChange);
            data.setDailyWinAmount(data.getDailyWinAmount() + actualChange);
        } else if (rawChange < 0) {
            actualChange = -Math.min(-rawChange, data.getSeasonCoin());
            data.setSeasonCoin(data.getSeasonCoin() + actualChange);
            data.setDailyLossAmount(data.getDailyLossAmount() - actualChange);
        }

        SeasonMatchRecord ownRecord = record(session, actualChange, now, false,
                session.getOpponentId(), session.getOpponentName(), session.getOpponentTierId());
        data.addMatchRecord(ownRecord, HISTORY_LIMIT);
        data.setRepresentativeGameType(session.getGameType());
        data.setRepresentativeStake(session.getStake());
        data.setRepresentativeSpinWins(new ArrayList<>(session.getPlayerSpinWins()));
        data.setActiveMatch(null);

        // 机器人对手: 只给发起方模拟对局, 不写 pending、不发对手奖励、不改任何第三方数据
        if (!RobotUtil.isRobot(session.getOpponentId())) {
            SeasonMatchRecord opponentRecord = record(session, -actualChange, now, true,
                    ctx.playerId(), data.getPlayerName(), data.getTierId());
            //matchId 每局新生成, 写不进去说明该 matchId 已存在待结算记录, 对手这笔币会丢
            if (!seasonPlayerDao.applyOpponentSettlement(session.getOpponentId(), data.getSeasonKey(),
                    session.getMatchId(), -actualChange, opponentRecord, HISTORY_LIMIT)) {
                log.warn("赛季对局对手待结算写入未生效, 对手币未变动 playerId={},opponentId={},matchId={},coinChange={}",
                        ctx.playerId(), session.getOpponentId(), session.getMatchId(), -actualChange);
            }
        }
        autoSaveService.enqueueSave(data);
        //对局结算改币且跨玩家写入, 按每日匹配次数封顶, 频率可控
        log.info("赛季对局结算 playerId={},matchId={},opponentId={},playerWin={},opponentWin={},rawChange={},coinChange={},seasonCoin={}",
                ctx.playerId(), session.getMatchId(), session.getOpponentId(),
                playerTotal, opponentTotal, rawChange, actualChange, data.getSeasonCoin());

        SeasonMatchResult result = new SeasonMatchResult();
        result.setMatchId(session.getMatchId());
        result.setResult(Long.compare(actualChange, 0));
        result.setPlayerTotalWin(playerTotal);
        result.setOpponentTotalWin(opponentTotal);
        result.setPlayerSpinWins(session.getPlayerSpinWins());
        result.setOpponentSpinWins(session.getOpponentSpinWins());
        result.setCoinChange(actualChange);
        result.setSeasonCoin(data.getSeasonCoin());
        return new CommonResult<>(Code.SUCCESS, result);
    }

    /**
     * 首场 PK 前使用当前赛季机台的连续 Spin 结果生成一次初始代表数据，避免所有玩家都因没有 PK
     * 历史而无法进入首场匹配。首份数据凑齐后不再由普通 Spin 覆盖，完成 PK 后仍以最新 PK 结果为准。
     * 只取赛季入口的旋转: 代表数据的下注额/收益要与匹配池的赛季币口径一致。
     */
    private void seedRepresentativeIfAbsent(SimPlayerContext ctx, int gameType, SpinStatInfo statInfo, int enterType) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (enterType != ENTER_TYPE_SEASON || data == null || data.seasonPhase() == null
                || data.seasonPhase() == SeasonPhase.NOVICE
                || statInfo == null || statInfo.getBet() <= 0) {
            return;
        }
        int expectedSpins = data.seasonPhase() == SeasonPhase.ADVANCED
                ? ADVANCED_SPIN_COUNT : LOOP_SPIN_COUNT;
        List<Long> wins = data.getRepresentativeSpinWins();
        if (wins.size() >= expectedSpins) {
            return;
        }
        SeasonStartCfg season = configService.season(data.getSeasonId());
        if (season == null || season.getAvailableGames() != gameType) {
            return;
        }
        if (!wins.isEmpty() && (data.getRepresentativeGameType() != gameType
                || data.getRepresentativeStake() != statInfo.getBet())) {
            wins.clear();
        }
        data.setRepresentativeGameType(gameType);
        data.setRepresentativeStake(statInfo.getBet());
        wins.add(Math.max(0, statInfo.getWin()));
        autoSaveService.enqueueSave(data);
    }

    /**
     * 亏损保护作用于“被匹配”概率。主动匹配仅在配置明确开启时应用；当前配置为 false，
     * 因此不会错误限制主动发起方。
     */
    int matchWeight(SeasonPlayerData candidate, SeasonMatchCfg cfg, boolean activeMatch) {
        if (activeMatch && !cfg.getActiveMatchAffected()) {
            return SeasonPolicy.RATIO_BASE;
        }
        long excessLoss = candidate.getDailyLossAmount() - cfg.getDailyMatchLossLimit();
        if (cfg.getDailyMatchLossLimit() < 0 || excessLoss < 0) {
            return SeasonPolicy.RATIO_BASE;
        }
        return SeasonPolicy.ratioFor(excessLoss, cfg.getExceedLossMatcProb());
    }

    private SeasonMatchRecord record(SeasonMatchSession session, long coinChange, long now,
                                     boolean opponent, long opponentId, String opponentName, int opponentTierId) {
        SeasonMatchRecord record = new SeasonMatchRecord();
        record.setMatchId(session.getMatchId());
        record.setOpponentId(opponentId);
        record.setOpponentName(opponentName);
        record.setOpponentTierId(opponentTierId);
        record.setGameType(session.getGameType());
        record.setStake(session.getStake());
        record.setPlayerSpinWins(opponent ? session.getOpponentSpinWins() : session.getPlayerSpinWins());
        record.setOpponentSpinWins(opponent ? session.getPlayerSpinWins() : session.getOpponentSpinWins());
        record.setResult(Long.compare(coinChange, 0));
        record.setCoinChange(coinChange);
        record.setFinishTime(now);
        return record;
    }

    private int currentDay(SeasonPlayerData data, long now) {
        return SeasonTimeline.currentDay(data.getStartTime(), now);
    }

    private long sum(List<Long> values) {
        long sum = 0;
        for (Long value : values) {
            if (value != null && value > 0) sum = Math.addExact(sum, value);
        }
        return sum;
    }

    private CommonResult<SeasonMatchSession> failStart(int code, SimPlayerContext ctx,
                                                       int gameType, long stake, String message) {
        log.warn("赛季匹配失败 playerId={},gameType={},stake={},code={},detail={}",
                ctx.playerId(), gameType, stake, code, message);
        return new CommonResult<>(code);
    }
}
