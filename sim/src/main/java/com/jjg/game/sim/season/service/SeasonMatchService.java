package com.jjg.game.sim.season.service;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.sampledata.bean.SeasonMatchCfg;
import com.jjg.game.sampledata.bean.SeasonStartCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.season.dao.SeasonPlayerDao;
import com.jjg.game.sim.season.data.SeasonMatchRecord;
import com.jjg.game.sim.season.data.SeasonMatchResult;
import com.jjg.game.sim.season.data.SeasonMatchSession;
import com.jjg.game.sim.season.data.SeasonPlayerData;
import com.jjg.game.sim.season.model.SeasonPhase;
import com.jjg.game.sim.service.SimAutoSaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 赛季异步匹配和基于 slots 上报结果的结算服务。
 */
@Service
public class SeasonMatchService {
    private static final Logger log = LoggerFactory.getLogger(SeasonMatchService.class);
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final int ADVANCED_SPIN_COUNT = 3;
    private static final int LOOP_SPIN_COUNT = 5;
    private static final int CANDIDATE_LIMIT = 20;
    private static final int HISTORY_LIMIT = 50;
    /**
     * 对局超时: 超时后按弃赛处理, 未完成的局按 0 收益补齐结算 (弃赛判负, 防止先看牌不利再放弃重开)。
     */
    private static final long MATCH_TIMEOUT_MILLIS = 30L * 60 * 1000;

    private final SeasonConfigService configService;
    private final SeasonPlayerDao seasonPlayerDao;
    private final SeasonEconomyService economyService;
    private final SimAutoSaveService autoSaveService;

    public SeasonMatchService(SeasonConfigService configService, SeasonPlayerDao seasonPlayerDao,
                              SeasonEconomyService economyService, SimAutoSaveService autoSaveService) {
        this.configService = configService;
        this.seasonPlayerDao = seasonPlayerDao;
        this.economyService = economyService;
        this.autoSaveService = autoSaveService;
    }

    public CommonResult<SeasonMatchSession> start(SimPlayerContext ctx, int gameType, long stake, long now) {
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
        if (data.getSeasonCoin() < stake) {
            return failStart(Code.NOT_ENOUGH, ctx, gameType, stake, "赛季币不足");
        }

        List<SeasonPlayerData> candidates = seasonPlayerDao.findMatchCandidates(
                data.getSeasonKey(), ctx.playerId(), gameType, stake, CANDIDATE_LIMIT);
        int expectedSpins = data.seasonPhase() == SeasonPhase.ADVANCED
                ? ADVANCED_SPIN_COUNT : LOOP_SPIN_COUNT;
        List<SeasonPlayerData> eligible = candidates.stream()
                .filter(candidate -> candidate.getRepresentativeSpinWins().size() >= expectedSpins)
                .toList();
        if (eligible.isEmpty()) {
            return failStart(Code.NOT_FOUND, ctx, gameType, stake, "没有可用对手");
        }
        //候选池内随机, 避免固定命中同一对手
        SeasonPlayerData opponent = eligible.get(RandomUtils.getRandomNumIntMax(eligible.size()) - 1);

        SeasonMatchSession session = new SeasonMatchSession();
        session.setMatchId(RandomUtils.getOriginalUUid());
        session.setOpponentId(opponent.getPlayerId());
        session.setGameType(gameType);
        session.setStake(stake);
        session.setExpectedSpins(expectedSpins);
        session.setStartedAt(now);
        session.setOpponentSpinWins(opponent.getRepresentativeSpinWins().subList(0, expectedSpins));
        data.setSeasonCoin(data.getSeasonCoin() - stake);
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
     * 热路径: 无对局、非本局游戏或无统计信息时静默跳过 (玩家可能同时在玩其他机台)。
     */
    public CommonResult<SeasonMatchResult> onSpin(SimPlayerContext ctx, int gameType, SpinStatInfo statInfo, long now) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonMatchSession session = data == null ? null : data.getActiveMatch();
        if (session == null) {
            return new CommonResult<>(Code.SUCCESS);
        }
        if (now - session.getStartedAt() >= MATCH_TIMEOUT_MILLIS) {
            //超时弃赛: 先补 0 结算旧局, 本次旋转不计入
            return settleExpired(ctx, session, now);
        }
        if (session.getGameType() != gameType || statInfo == null) {
            return new CommonResult<>(Code.SUCCESS);
        }
        if (session.getPlayerSpinWins().size() < session.getExpectedSpins()) {
            session.getPlayerSpinWins().add(Math.max(0, statInfo.getWin()));
        }
        if (session.getPlayerSpinWins().size() < session.getExpectedSpins()) {
            return new CommonResult<>(Code.SUCCESS);
        }
        return settle(ctx, session, now);
    }

    /**
     * 进行中的对局超时则按弃赛补 0 结算; 匹配入口调用, 保证玩家不会被残留对局永久卡死。
     */
    public void settleIfExpired(SimPlayerContext ctx, long now) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonMatchSession session = data == null ? null : data.getActiveMatch();
        if (session != null && now - session.getStartedAt() >= MATCH_TIMEOUT_MILLIS) {
            settleExpired(ctx, session, now);
        }
    }

    private CommonResult<SeasonMatchResult> settleExpired(SimPlayerContext ctx, SeasonMatchSession session, long now) {
        log.info("赛季对局超时弃赛结算 playerId={},matchId={},recorded={}",
                ctx.playerId(), session.getMatchId(), session.getPlayerSpinWins().size());
        while (session.getPlayerSpinWins().size() < session.getExpectedSpins()) {
            session.getPlayerSpinWins().add(0L);
        }
        return settle(ctx, session, now);
    }

    private CommonResult<SeasonMatchResult> settle(SimPlayerContext ctx, SeasonMatchSession session, long now) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (!data.markMatchProcessed(session.getMatchId(), HISTORY_LIMIT)) {
            data.setActiveMatch(null);
            return new CommonResult<>(Code.REPEAT_OP);
        }
        long playerTotal = sum(session.getPlayerSpinWins());
        long opponentTotal = sum(session.getOpponentSpinWins());
        long rawChange = playerTotal - opponentTotal;
        data.setSeasonCoin(data.getSeasonCoin() + session.getStake());
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

        SeasonMatchRecord ownRecord = record(session, actualChange, now, false, session.getOpponentId());
        data.addMatchRecord(ownRecord, HISTORY_LIMIT);
        data.setRepresentativeGameType(session.getGameType());
        data.setRepresentativeStake(session.getStake());
        data.setRepresentativeSpinWins(new ArrayList<>(session.getPlayerSpinWins()));
        data.setActiveMatch(null);

        SeasonMatchRecord opponentRecord = record(session, -actualChange, now, true, ctx.playerId());
        seasonPlayerDao.applyOpponentSettlement(session.getOpponentId(), data.getSeasonKey(),
                session.getMatchId(), -actualChange, opponentRecord, HISTORY_LIMIT);
        autoSaveService.enqueueSave(data);

        SeasonMatchResult result = new SeasonMatchResult();
        result.setMatchId(session.getMatchId());
        result.setResult(Long.compare(actualChange, 0));
        result.setPlayerTotalWin(playerTotal);
        result.setOpponentTotalWin(opponentTotal);
        result.setCoinChange(actualChange);
        result.setSeasonCoin(data.getSeasonCoin());
        return new CommonResult<>(Code.SUCCESS, result);
    }

    private SeasonMatchRecord record(SeasonMatchSession session, long coinChange, long now,
                                     boolean opponent, long opponentId) {
        SeasonMatchRecord record = new SeasonMatchRecord();
        record.setMatchId(session.getMatchId());
        record.setOpponentId(opponentId);
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
        return (int) ((Math.max(now, data.getStartTime()) - data.getStartTime()) / DAY_MILLIS) + 1;
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
