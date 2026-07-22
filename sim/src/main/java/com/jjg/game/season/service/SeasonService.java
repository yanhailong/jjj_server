package com.jjg.game.season.service;

import com.jjg.game.core.base.condition.numeric.GameConditionEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.SeasonGemCfg;
import com.jjg.game.sampledata.bean.SeasonShopCfg;
import com.jjg.game.sampledata.bean.SeasonStartCfg;
import com.jjg.game.sampledata.bean.SeasonTierCfg;
import com.jjg.game.sampledata.bean.TaskCfg;
import com.jjg.game.season.config.SeasonTrialDef;
import com.jjg.game.season.data.*;
import com.jjg.game.season.model.SeasonSnapshot;
import com.jjg.game.season.pb.res.*;
import com.jjg.game.season.pb.struct.*;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.service.SimConditionEventFactory;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.service.SimAutoSaveService;
import com.jjg.game.social.service.SocialSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 赛季协议层门面，集中完成领域对象到客户端结构的转换。
 */
@Service
public class SeasonService implements SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(SeasonService.class);
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final DateTimeFormatter GM_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter GM_RESULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SeasonLifecycleService lifecycleService;
    private final SeasonConfigService configService;
    private final SeasonShopService shopService;
    private final SeasonGemService gemService;
    private final SeasonMatchService matchService;
    private final SeasonDropService dropService;
    private final SeasonRankingService rankingService;
    private final PlayerPackService playerPackService;
    private final SeasonTrialService trialService;
    private final SimAutoSaveService autoSaveService;
    private final SocialSender socialSender;
    private final SeasonFreeGameService freeGameService;

    public SeasonService(SeasonLifecycleService lifecycleService, SeasonConfigService configService,
                         SeasonShopService shopService, SeasonGemService gemService,
                         SeasonMatchService matchService, SeasonDropService dropService,
                         SeasonRankingService rankingService, PlayerPackService playerPackService,
                         SeasonTrialService trialService, SimAutoSaveService autoSaveService,
                         SocialSender socialSender, SeasonFreeGameService freeGameService) {
        this.lifecycleService = lifecycleService;
        this.configService = configService;
        this.shopService = shopService;
        this.gemService = gemService;
        this.matchService = matchService;
        this.dropService = dropService;
        this.rankingService = rankingService;
        this.playerPackService = playerPackService;
        this.trialService = trialService;
        this.autoSaveService = autoSaveService;
        this.socialSender = socialSender;
        this.freeGameService = freeGameService;
    }

    public ResSeasonInfo info(SimPlayerContext ctx) {
        ResSeasonInfo response = new ResSeasonInfo(Code.SUCCESS);
        long systemTime = System.currentTimeMillis();
        SeasonSnapshot snapshot = lifecycleService.ensureCurrent(ctx, systemTime);
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        //进入赛季视为掉线回归: 循环赛季对局中掉线超时的, 在此按模拟数据自动补完并结算
        SeasonMatchResult offlineResult = matchService.settleOfflineMatch(
                ctx, lifecycleService.currentTime(ctx, systemTime));
        if (offlineResult != null) {
            socialSender.sendTo(ctx.playerId(), matchNotify(offlineResult));
        }
        SeasonStartCfg cfg = configService.season(snapshot.seasonId());
        SeasonInfo info = new SeasonInfo();
        info.seasonId = snapshot.seasonId();
        info.phase = snapshot.phase().ordinal() + 1;
        info.cycleIndex = snapshot.cycleIndex();
        info.nameLanguageId = cfg == null ? 0 : cfg.getSeasonName();
        info.startTime = snapshot.startTime();
        info.endTime = snapshot.endTime();
        info.day = snapshot.day();
        info.gameType = cfg == null ? 0 : cfg.getAvailableGames();
        info.seasonCoin = data.getSeasonCoin();
        info.totalEarnedCoin = data.getTotalEarnedCoin();
        info.tierId = data.getTierId();
        info.nextTierNeedCoin = nextTierNeedCoin(data);
        info.dailyMatchCount = data.getDailyMatchCount();
        info.dailyWinAmount = data.getDailyWinAmount();
        info.dailyLossAmount = data.getDailyLossAmount();
        info.rank = rankingService.rankOf(data);
        info.trialStars = data.getTrialStars().entrySet().stream().map(entry -> {
            KVInfo kv = new KVInfo();
            kv.key = entry.getKey();
            kv.value = entry.getValue();
            return kv;
        }).toList();

        info.rtp = cfg.getGameRTP();
        info.maxMultiplier = cfg.getMaxMultiplier();
        info.betRangeBegin = cfg.getBetRange().get(0);
        info.betRangeEnd = cfg.getBetRange().get(1);
        info.featureName = cfg.getFeatureName();
        info.openminigames = cfg.getOpenminigame();

        if(info.phase > 1){
            info.freeGameCount = freeGameService.freeGameCount();
            info.remainFreeGameCount = freeGameService.remainFreeGameCount(data);
        }
        //跨赛季后首次请求: 下发上赛季结算信息, 下发即清除
        SeasonSettlement lastSettlement = data.getLastSettlement();
        if (lastSettlement != null) {
            response.lastSettlement = settlementInfo(lastSettlement);
            data.setLastSettlement(null);
            autoSaveService.enqueueSave(data);
        }
        response.info = info;
        return response;
    }

    private SeasonSettlementInfo settlementInfo(SeasonSettlement settlement) {
        SeasonSettlementInfo info = new SeasonSettlementInfo();
        info.seasonId = settlement.getSeasonId();
        SeasonStartCfg cfg = configService.season(settlement.getSeasonId());
        info.nameLanguageId = cfg == null ? 0 : cfg.getSeasonName();
        info.phase = settlement.getPhase();
        info.cycleIndex = settlement.getCycleIndex();
        info.rank = settlement.getRank();
        info.tierId = settlement.getTierId();
        info.totalEarnedCoin = settlement.getTotalEarnedCoin();
        info.totalTrialStars = settlement.getTotalTrialStars();
        info.rewards = settlement.getRewards().isEmpty()
                ? List.of() : ItemUtils.buildItemInfo(settlement.getRewards());
        info.initialCoin = settlement.getInitialCoin();
        info.returnCoinMax = GameDataManager.getGlobalConfigCfg(SimConstant.Global.ID_RETURN_COIN_MAX).getIntValue();
        info.seasonBadge = settlement.getSeasonBadge();
        return info;
    }

    public ResSeasonShop shop(SimPlayerContext ctx) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        ResSeasonShop response = new ResSeasonShop(Code.SUCCESS);
        response.items = configService.shops(data.seasonPhase()).stream().map(cfg -> shopInfo(cfg, data)).toList();
        response.seasonCoin = data.getSeasonCoin();
        return response;
    }

    public ResSeasonBuy buy(SimPlayerContext ctx, int shopId, int count) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        CommonResult<Map<Integer, Long>> result = shopService.buy(ctx, shopId, count);
        ResSeasonBuy response = new ResSeasonBuy(result.code);
        if (result.data != null && !result.data.isEmpty()) {
            response.goods = ItemUtils.buildItemInfo(result.data);
        }
        response.seasonCoin = ctx.getSeasonPlayerData().getSeasonCoin();
        SeasonShopCfg cfg = configService.shop(shopId, ctx.getSeasonPlayerData().seasonPhase());
        response.purchased = cfg != null && cfg.getResetDaily()
                ? ctx.getSeasonPlayerData().getDailyShopPurchases().getOrDefault(shopId, 0)
                : ctx.getSeasonPlayerData().getShopPurchases().getOrDefault(shopId, 0);
        return response;
    }

    public ResSeasonGems gems(SimPlayerContext ctx) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        ResSeasonGems response = new ResSeasonGems(Code.SUCCESS);
        PlayerPack pack = playerPackService.getFromAllDB(ctx.playerId());
        Map<Integer, Integer> equipped = ctx.getSeasonPlayerData().getEquippedGems();
        response.gems = configService.gems().stream().map(cfg -> gemInfo(cfg, pack, equipped)).toList();
        response.slots = slots(equipped);
        return response;
    }

    public ResSeasonEquipGem equip(SimPlayerContext ctx, int slot, int itemId) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        CommonResult<Map<Integer, Integer>> result = gemService.equip(ctx, slot, itemId);
        ResSeasonEquipGem response = new ResSeasonEquipGem(result.code);
        response.slots = slots(result.data == null ? ctx.getSeasonPlayerData().getEquippedGems() : result.data);
        return response;
    }

    public ResSeasonCraftGem craft(SimPlayerContext ctx, List<Integer> itemIds, int keepItemId) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        CommonResult<SeasonCraftResult> result = gemService.craft(ctx, itemIds, keepItemId);
        ResSeasonCraftGem response = new ResSeasonCraftGem(result.code);
        if (result.data != null) {
            response.success = result.data.isSuccess();
            response.resultItemId = result.data.getResultItemId();
            response.resultCount = result.data.getResultCount();
            response.keptItemId = result.data.getKeptItemId();
        }
        response.seasonCoin = ctx.getSeasonPlayerData().getSeasonCoin();
        return response;
    }

    public ResSeasonMatch match(SimPlayerContext ctx, int gameType, long stake) {
        ResSeasonMatch response = new ResSeasonMatch(Code.SUCCESS);
        long systemTime = System.currentTimeMillis();
        lifecycleService.ensureCurrent(ctx, systemTime);
        CommonResult<SeasonMatchSession> result = matchService.start(
                ctx, gameType, stake, lifecycleService.currentTime(ctx, systemTime));

        if (result.success() && result.data != null) {
            response.matchId = result.data.getMatchId();
            response.opponentId = result.data.getOpponentId();
            response.opponentName = result.data.getOpponentName();
            response.opponentHeadImgId = result.data.getOpponentHeadImgId();
            response.opponentHeadFrameId = result.data.getOpponentHeadFrameId();
            response.gameType = result.data.getGameType();
            response.stake = result.data.getStake();
            response.expectedSpins = result.data.getExpectedSpins();
            response.opponentSpinWins = result.data.getOpponentSpinWins();
        } else {
            response.matchId = "0";
        }
        response.seasonCoin = ctx.getSeasonPlayerData().getSeasonCoin();
        return response;
    }

    /**
     * 升级到下一段位所需的累计赛季币 (下一档 RankRange 下限); 已是最高段位或无下一档时返回 0。
     */
    private long nextTierNeedCoin(SeasonPlayerData data) {
        int next = data.getTierId() + 1;
        SeasonTierCfg seasonTierCfg = GameDataManager.getSeasonTierCfg(next);
        if(seasonTierCfg == null){
            return 0;
        }
        Integer allCoin = seasonTierCfg.getRankRange().get(1);
        if(allCoin == null){
            return 0;
        }
        return allCoin - data.getTotalEarnedCoin();
    }

    /**
     * 段位升级奖励列表: 升段时奖励已由 SeasonEconomyService 自动发放, obtained 即已达到该段位。
     */
    public ResSeasonTierUpRewards tierUpRewards(SimPlayerContext ctx) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        ResSeasonTierUpRewards response = new ResSeasonTierUpRewards(Code.SUCCESS);
        response.tierId = data.getTierId();
        response.rewards = tierRewardInfos(data, SeasonTierCfg::getRankUpReward);
        return response;
    }

    /**
     * 段位赛季结算奖励列表: 结算时只按最终段位发放, obtained 表示已达到该段位。
     */
    public ResSeasonTierSettlementRewards tierSettlementRewards(SimPlayerContext ctx) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        ResSeasonTierSettlementRewards response = new ResSeasonTierSettlementRewards(Code.SUCCESS);
        response.tierId = data.getTierId();
        response.rewards = tierRewardInfos(data, SeasonTierCfg::getSettlementReward);
        return response;
    }

    private List<SeasonTierRewardInfo> tierRewardInfos(SeasonPlayerData data,
                                                       Function<SeasonTierCfg, Map<Integer, Long>> rewardGetter) {
        List<SeasonTierCfg> tiers = configService.tiers(data.seasonPhase());
        int currentIndex = -1;
        for (int index = 0; index < tiers.size(); index++) {
            if (tiers.get(index).getId() == data.getTierId()) {
                currentIndex = index;
                break;
            }
        }
        List<SeasonTierRewardInfo> result = new ArrayList<>(tiers.size());
        for (int index = 0; index < tiers.size(); index++) {
            SeasonTierCfg cfg = tiers.get(index);
            SeasonTierRewardInfo info = new SeasonTierRewardInfo();
            info.tierId = cfg.getId();
            info.rankLanguageId = cfg.getRank();
            Map<Integer, Long> reward = rewardGetter.apply(cfg);
            info.rewards = reward == null || reward.isEmpty() ? List.of() : ItemUtils.buildItemInfo(reward);
            info.obtained = index <= currentIndex;
            info.seasonBadge = cfg.getSeasonBadge();
            result.add(info);
        }
        return result;
    }

    public ResSeasonMatchHistory history(SimPlayerContext ctx) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        ResSeasonMatchHistory response = new ResSeasonMatchHistory(Code.SUCCESS);
        response.records = ctx.getSeasonPlayerData().getMatchHistory().stream().map(this::recordInfo).toList();
        return response;
    }

    public ResSeasonRank rank(SimPlayerContext ctx, int requestedLimit) {
        SeasonSnapshot snapshot = lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        int configuredLimit = configService.rankingLimit(data.seasonPhase());
        int limit = requestedLimit <= 0 ? configuredLimit : Math.min(requestedLimit, configuredLimit);
        ResSeasonRank response = new ResSeasonRank(Code.SUCCESS);
        response.entries = rankingService.ranking(data.getSeasonKey(), Math.max(1, limit)).stream()
                .map(this::rankInfo).toList();
        SeasonRankInfo selfRankInfo = new SeasonRankInfo();
        selfRankInfo.rank = rankingService.rankOf(data);
        selfRankInfo.playerId = data.getPlayerId();
        selfRankInfo.playerName = data.getPlayerName();
        selfRankInfo.seasonCoin = data.getSeasonCoin();
        selfRankInfo.totalEarnedCoin = data.getTotalEarnedCoin();
        // 与 SeasonInfo.phase 一致：1新手，2进阶，3循环
        selfRankInfo.phase = snapshot.phase().ordinal() + 1;
        response.selfRankInfo = selfRankInfo;
        response.endTime = snapshot.endTime();
        return response;
    }

    /**
     * slots 普通旋转成功后的赛季联动；无对局结算时不产生通知。内部吞异常, 不影响 slots 主流程。
     */
    public NotifySeasonMatchResult onSpin(SimPlayerContext ctx, int gameType, SpinStatInfo statInfo) {
        GameConditionEvent event = statInfo == null ? null : SimConditionEventFactory.fromSpin(
                gameType, statInfo.getMultiple(), 0, statInfo);
        return onSpin(ctx, gameType, statInfo, event);
    }

    /** 生产热路径复用同一个条件事件，其他赛季结算仍使用原始 SpinStatInfo。 */
    public NotifySeasonMatchResult onSpin(SimPlayerContext ctx, int gameType, SpinStatInfo statInfo,
                                          GameConditionEvent event) {
        try {
            dropService.onSpin(ctx, gameType);
            //试炼挑战窗口推进; 结算时直接下发通知 (与对局互斥: 试炼仅新手赛季, 对局仅进阶/循环赛季)
            SeasonTrialResult trialResult = event == null ? null : trialService.onSpin(ctx, gameType, event);
            if (trialResult != null) {
                socialSender.sendTo(ctx.playerId(), trialNotify(trialResult));
            }
            long systemTime = System.currentTimeMillis();
            CommonResult<SeasonMatchResult> result = matchService.onSpin(
                    ctx, gameType, statInfo, lifecycleService.currentTime(ctx, systemTime));
            if (result.data == null) {
                //非本局游戏/无对局/重复结算等场景静默跳过, 不向客户端下发错误通知
                if (!result.success()) {
                    log.warn("赛季对局旋转结算跳过 playerId={},gameType={},code={}", ctx.playerId(), gameType, result.code);
                }
                return null;
            }
            NotifySeasonMatchResult notify = matchNotify(result.data);
            socialSender.sendTo(ctx.playerId(), notify);
            return notify;
        } catch (Exception e) {
            log.error("赛季旋转联动异常 playerId={},gameType={}", ctx.playerId(), gameType, e);
            return null;
        }
    }

    /**
     * 试炼任务列表 (仅新手赛季有内容)。被动型关卡惰性判定达成新星级时, 随本次列表一并下发结算通知。
     */
    public ResSeasonTrials trials(SimPlayerContext ctx) {
        long systemTime = System.currentTimeMillis();
        SeasonSnapshot snapshot = lifecycleService.ensureCurrent(ctx, systemTime);
        long now = lifecycleService.currentTime(ctx, systemTime);
        ResSeasonTrials response = new ResSeasonTrials(Code.SUCCESS);
        List<SeasonTrialStatus> statuses = trialService.list(ctx, snapshot, now);
        response.trials = statuses.stream().map(this::trialInfo).toList();
        SeasonTrialSession session = ctx.getSeasonPlayerData().getActiveTrial();
        response.activeTrialId = session == null ? 0 : session.getTrialId();
        for (SeasonTrialStatus status : statuses) {
            if (status.getPassiveResult() != null) {
                socialSender.sendTo(ctx.playerId(), trialNotify(status.getPassiveResult()));
            }
        }
        return response;
    }

    /**
     * slots 每次旋转结束后查询当前试炼进度。
     */
    public ResSeasonTrialProgress trialProgress(SimPlayerContext ctx) {
        ResSeasonTrialProgress response = new ResSeasonTrialProgress(Code.SUCCESS);
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonTrialSession session = data == null ? null : data.getActiveTrial();
        if (session != null) {
            response.spinCount = session.getSpinCount();
            response.progress = session.getProgress();
        }
        return response;
    }

    /**
     * 发起试炼挑战。
     */
    public ResSeasonTrialChallenge trialChallenge(SimPlayerContext ctx, int trialId) {
        long systemTime = System.currentTimeMillis();
        SeasonSnapshot snapshot = lifecycleService.ensureCurrent(ctx, systemTime);
        long now = lifecycleService.currentTime(ctx, systemTime);
        CommonResult<SeasonTrialSession> result = trialService.challenge(ctx, trialId, snapshot, now);
        ResSeasonTrialChallenge response = new ResSeasonTrialChallenge(result.code);
        response.trialId = trialId;
        if (result.data != null) {
            SeasonTrialDef def = trialService.trialDef(result.data.getTrialId());
            response.expectedSpins = def == null ? 0 : def.windowSpins();
        }
        return response;
    }

    private SeasonTrialInfo trialInfo(SeasonTrialStatus status) {
        SeasonTrialInfo info = new SeasonTrialInfo();
        SeasonTrialDef def = status.getDef();
        info.trialId = def.trialId();
        info.day = def.day();
        info.unlocked = status.isUnlocked();
        info.stars = status.getStars();

        if (def.starTasks() != null && !def.starTasks().isEmpty()) {
            info.taskIds = new ArrayList<>();
            for (TaskCfg cfg : def.starTasks()) {
                KVInfo kvInfo = new KVInfo(cfg.getId(), cfg.getTaskConditionId().getLast().intValue());
                info.taskIds.add(kvInfo);
            }
        }
        info.active = status.isActive();
        info.spinCount = status.getSpinCount();
        info.expectedSpins = def.windowSpins();
        info.progress = status.getProgress();
        return info;
    }

    private NotifySeasonTrialResult trialNotify(SeasonTrialResult result) {
        NotifySeasonTrialResult notify = new NotifySeasonTrialResult(Code.SUCCESS);
        notify.trialId = result.getTrialId();
        notify.achievedStars = result.getAchievedStars();
        notify.bestStars = result.getBestStars();
        notify.rewards = result.getRewards() == null || result.getRewards().isEmpty()
                ? List.of() : ItemUtils.buildItemInfo(result.getRewards());
        notify.spinCount = result.getSpinCount();
        notify.progress = result.getProgress();
        notify.seasonCoin = result.getSeasonCoin();
        return notify;
    }

    /**
     * 玩家 tick: 超时对局按弃赛结算并通知, 玩家不旋转/不再匹配时押金也能按时释放。
     */
    @Override
    public void onTick(SimPlayerContext ctx, long systemTime) {
        long now = lifecycleService.currentTime(ctx, systemTime);
        SeasonMatchResult result = matchService.settleIfExpired(ctx, now);
        if (result != null) {
            socialSender.sendTo(ctx.playerId(), matchNotify(result));
        }
    }

    /**
     * 先于生命周期切季 (order=100) 结算超时对局, 避免残留对局被切季直接清掉。
     */
    @Override
    public int order() {
        return 90;
    }

    /**
     * 调整当前玩家的赛季测试时间。addday/settime 只能推进到未来；resettime 仅允许在当前赛季内恢复系统时间。
     */
    public CommonResult<String> gmTime(SimPlayerContext ctx, String[] orders) {
        if (ctx == null || ctx.getSeasonPlayerData() == null || orders == null || orders.length < 2) {
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        long systemTime = System.currentTimeMillis();
        try {
            if ("addday".equalsIgnoreCase(orders[1])) {
                if (orders.length != 3) {
                    return new CommonResult<>(Code.PARAM_ERROR);
                }
                long days = Long.parseLong(orders[2]);
                if (days <= 0) {
                    log.warn("赛季 GM addday 参数必须为正数 playerId={},days={}", ctx.playerId(), days);
                    return new CommonResult<>(Code.PARAM_ERROR);
                }
                long delta = Math.multiplyExact(days, DAY_MILLIS);
                data.setGmTimeOffset(Math.addExact(data.getGmTimeOffset(), delta));
            } else if ("settime".equalsIgnoreCase(orders[1])) {
                if (orders.length != 3) {
                    return new CommonResult<>(Code.PARAM_ERROR);
                }
                LocalDate targetDate = LocalDate.parse(orders[2], GM_DATE_FORMATTER);
                long targetTime = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (targetTime <= lifecycleService.currentTime(ctx, systemTime)) {
                    log.warn("赛季 GM settime 不允许回拨 playerId={},date={}", ctx.playerId(), orders[2]);
                    return new CommonResult<>(Code.PARAM_ERROR);
                }
                data.setGmTimeOffset(Math.subtractExact(targetTime, systemTime));
            } else if ("resettime".equalsIgnoreCase(orders[1])) {
                if (orders.length != 2) {
                    return new CommonResult<>(Code.PARAM_ERROR);
                }
                if (data.getGmTimeOffset() != 0) {
                    if (data.getActiveMatch() != null) {
                        log.warn("赛季 GM resettime 执行失败，存在进行中的对局 playerId={},matchId={}",
                                ctx.playerId(), data.getActiveMatch().getMatchId());
                        return new CommonResult<>(Code.REPEAT_OP);
                    }
                    if (!lifecycleService.canResetTime(ctx, systemTime)) {
                        log.warn("赛季 GM resettime 执行失败，恢复系统时间会跨赛季 playerId={},seasonKey={}",
                                ctx.playerId(), data.getSeasonKey());
                        return new CommonResult<>(Code.PARAM_ERROR);
                    }
                    data.setGmTimeOffset(0L);
                    data.setLastMatchTime(0L);
                }
            } else {
                return new CommonResult<>(Code.PARAM_ERROR);
            }
        } catch (NumberFormatException | DateTimeParseException | ArithmeticException e) {
            log.warn("赛季 GM 时间参数错误 playerId={},orders={}", ctx.playerId(), orders, e);
            return new CommonResult<>(Code.PARAM_ERROR);
        }

        data.setLastPendingCheckTime(0L);
        onTick(ctx, systemTime);
        SeasonSnapshot snapshot = lifecycleService.ensureCurrent(ctx, systemTime);
        autoSaveService.enqueueSave(data);
        long now = lifecycleService.currentTime(ctx, systemTime);
        String current = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(GM_RESULT_FORMATTER);
        return new CommonResult<>(Code.SUCCESS, "seasonId=" + snapshot.seasonId()
                + ",day=" + snapshot.day() + ",time=" + current);
    }

    private NotifySeasonMatchResult matchNotify(SeasonMatchResult result) {
        NotifySeasonMatchResult notify = new NotifySeasonMatchResult(Code.SUCCESS);
        notify.matchId = result.getMatchId();
        notify.result = result.getResult();
        notify.playerTotalWin = result.getPlayerTotalWin();
        notify.opponentTotalWin = result.getOpponentTotalWin();
        notify.playerSpinWins = result.getPlayerSpinWins();
        notify.opponentSpinWins = result.getOpponentSpinWins();
        notify.coinChange = result.getCoinChange();
        notify.seasonCoin = result.getSeasonCoin();
        return notify;
    }

    private SeasonShopItemInfo shopInfo(SeasonShopCfg cfg, SeasonPlayerData data) {
        SeasonShopItemInfo info = new SeasonShopItemInfo();
        info.id = cfg.getId();
        info.order = cfg.getOrder();
        info.goods = cfg.getGoods() == null ? List.of() : ItemUtils.buildItemInfo(cfg.getGoods());
        info.cost = cfg.getCost() == null ? List.of() : ItemUtils.buildItemInfo(cfg.getCost());
        info.resetDaily = cfg.getResetDaily();
        info.purchaseLimit = cfg.getDailyPurchaseLimit();
        info.purchased = (cfg.getResetDaily() ? data.getDailyShopPurchases() : data.getShopPurchases())
                .getOrDefault(cfg.getId(), 0);
        info.languageId = cfg.getLanguage();
        info.icon = cfg.getIcon();
        return info;
    }

    private SeasonGemInfo gemInfo(SeasonGemCfg cfg, PlayerPack pack, Map<Integer, Integer> equipped) {
        SeasonGemInfo info = new SeasonGemInfo();
        info.configId = cfg.getId();
        info.itemId = cfg.getItemId();
        info.type = cfg.getType();
        info.genre = cfg.getGenre();
        info.rarity = cfg.getRarity();
//        info.buff = cfg.getBuff();
        info.count = pack == null ? 0 : pack.getItemCount(cfg.getItemId());
        info.equippedCount = (int) equipped.values().stream().filter(item -> item == cfg.getItemId()).count();
        info.nameLanguageId = cfg.getGemName();
        info.descLanguageId = cfg.getGemDesc();
        return info;
    }

    private List<SeasonGemSlotInfo> slots(Map<Integer, Integer> equipped) {
        List<SeasonGemSlotInfo> result = new ArrayList<>(equipped.size());
        equipped.forEach((slot, itemId) -> {
            SeasonGemSlotInfo info = new SeasonGemSlotInfo();
            info.slot = slot;
            info.itemId = itemId;
            result.add(info);
        });
        result.sort(java.util.Comparator.comparingInt(info -> info.slot));
        return result;
    }

    private SeasonMatchRecordInfo recordInfo(SeasonMatchRecord record) {
        SeasonMatchRecordInfo info = new SeasonMatchRecordInfo();
        info.matchId = record.getMatchId();
        info.opponentId = record.getOpponentId();
        info.opponentName = record.getOpponentName();
        info.opponentTierId = record.getOpponentTierId();
        info.gameType = record.getGameType();
        info.stake = record.getStake();
        info.playerSpinWins = record.getPlayerSpinWins();
        info.opponentSpinWins = record.getOpponentSpinWins();
        info.result = record.getResult();
        info.coinChange = record.getCoinChange();
        info.finishTime = record.getFinishTime();
        return info;
    }

    private SeasonRankInfo rankInfo(SeasonRankEntry entry) {
        SeasonRankInfo info = new SeasonRankInfo();
        info.rank = entry.getRank();
        info.playerId = entry.getPlayerId();
        info.playerName = entry.getPlayerName();
        info.seasonCoin = entry.getSeasonCoin();
        info.totalEarnedCoin = entry.getTotalEarnedCoin();
        info.phase = entry.getPhase();
        return info;
    }
}
