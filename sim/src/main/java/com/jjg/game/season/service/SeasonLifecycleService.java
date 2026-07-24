package com.jjg.game.season.service;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.bean.SeasonMatchCfg;
import com.jjg.game.sampledata.bean.SeasonRankingCfg;
import com.jjg.game.sampledata.bean.SeasonTierCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.season.dao.SeasonPlayerDao;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.data.SeasonPendingSettlement;
import com.jjg.game.season.data.SeasonSettlement;
import com.jjg.game.season.model.SeasonSnapshot;
import com.jjg.game.sim.service.SimAutoSaveService;
import com.jjg.game.sim.service.SimConfigCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 维护玩家个人赛季时间线，并在登录和在线跨季时完成状态切换。
 */
@Service
public class SeasonLifecycleService implements SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(SeasonLifecycleService.class);
    /**
     * 跨节点待结算记录的拉取节流; 登录后首次访问必拉, 其后最多每隔该间隔查一次库。
     */
    private static final long PENDING_CHECK_INTERVAL_MILLIS = 60_000L;

    private final SeasonConfigService configService;
    private final SeasonTimeline timeline = new SeasonTimeline();
    private final CorePlayerService corePlayerService;
    private final SeasonPlayerDao seasonPlayerDao;
    private final SeasonRankingService rankingService;
    private final PlayerPackService playerPackService;
    private final SeasonEconomyService economyService;
    private final MailService mailService;
    private final SimAutoSaveService autoSaveService;
    private final SimConfigCacheService simConfigCacheService;

    public SeasonLifecycleService(SeasonConfigService configService, CorePlayerService corePlayerService,
                                  SeasonPlayerDao seasonPlayerDao, SeasonRankingService rankingService,
                                  PlayerPackService playerPackService, SeasonEconomyService economyService,
                                  MailService mailService, SimAutoSaveService autoSaveService,
                                  SimConfigCacheService simConfigCacheService) {
        this.configService = configService;
        this.corePlayerService = corePlayerService;
        this.seasonPlayerDao = seasonPlayerDao;
        this.rankingService = rankingService;
        this.playerPackService = playerPackService;
        this.economyService = economyService;
        this.mailService = mailService;
        this.autoSaveService = autoSaveService;
        this.simConfigCacheService = simConfigCacheService;
    }

    /**
     * 将系统时间换算为玩家的赛季时间。GM 偏移仅影响赛季业务，不影响其他系统。
     */
    public long currentTime(SimPlayerContext ctx, long systemTime) {
        SeasonPlayerData data = ctx == null ? null : ctx.getSeasonPlayerData();
        return Math.addExact(systemTime, data == null ? 0L : data.getGmTimeOffset());
    }

    /**
     * 判断恢复系统时间后是否仍处于当前赛季，避免回拨触发切季结算。
     */
    public boolean canResetTime(SimPlayerContext ctx, long systemTime) {
        SeasonPlayerData data = ctx == null ? null : ctx.getSeasonPlayerData();
        if (data == null || data.getSeasonKey() == null || data.getGmTimeOffset() == 0) {
            return true;
        }
        long origin = resolveTimelineOrigin(ctx, data);
        SeasonSnapshot snapshot = timeline.resolve(origin, systemTime, configService.definitions());
        return data.getSeasonKey().equals(snapshot.seasonKey());
    }

    public SeasonSnapshot ensureCurrent(SimPlayerContext ctx, long systemTime) {
        return ensureCurrentAt(ctx, currentTime(ctx, systemTime));
    }

    private SeasonSnapshot ensureCurrentAt(SimPlayerContext ctx, long now) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (data == null) {
            data = new SeasonPlayerData();
            data.setPlayerId(ctx.playerId());
            ctx.setSeasonPlayerData(data);
        }
        refreshPlayerName(ctx, data);
        if (data.getSeasonKey() != null) {
            data.resetDaily(dailyKey(now));
        }
        applyPendingSettlements(ctx, data, now);
        if (data.getSeasonKey() != null && now >= data.getStartTime() && now < data.getEndTime()) {
            return snapshot(data, now);
        }
        long origin = resolveTimelineOrigin(ctx, data);
        SeasonSnapshot snapshot = timeline.resolve(origin, now, configService.definitions());
        boolean changed = !snapshot.seasonKey().equals(data.getSeasonKey());
        if (changed) {
            //切季前绕过节流强制消费一次待结算, 避免节流窗口内新到的记录被切季清理误删
            data.setLastPendingCheckTime(0);
            applyPendingSettlements(ctx, data, now);
            String oldSeasonKey = data.getSeasonKey();
            long initialCoin = settlePreviousSeason(ctx, data);
            data.startSeason(snapshot, initialCoin);
            log.info("玩家赛季切换 playerId={},oldSeasonKey={},seasonId={},seasonKey={},initialCoin={}",
                    ctx.playerId(), oldSeasonKey, snapshot.seasonId(), snapshot.seasonKey(), initialCoin);
        }
        data.resetDaily(dailyKey(now));
        if (changed) {
            autoSaveService.enqueueSave(data);
            //切季后旧 seasonKey 的待结算记录不再可消费, 一并清理 (IO 线程执行, FIFO 在数据落库之后)
            long playerId = ctx.playerId();
            autoSaveService.enqueueTask(() -> seasonPlayerDao.deletePendingSettlementsByPlayer(playerId));
        }
        return snapshot;
    }

    @Override
    public void onTick(SimPlayerContext ctx, long systemTime) {
        long now = currentTime(ctx, systemTime);
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (data != null && now < data.getEndTime() && data.getDailyKey() == dailyKey(now)) {
            return;
        }
        ensureCurrentAt(ctx, now);
    }

    /**
     * 昵称/头像冗余在赛季文档上供匹配/榜单直接读取; 变更后在下一次协议入口同步并落库。
     */
    private void refreshPlayerName(SimPlayerContext ctx, SeasonPlayerData data) {
        Player player = ctx.getPlayerController() == null ? null : ctx.getPlayerController().getPlayer();
        if (player == null) {
            return;
        }
        boolean changed = false;
        if (player.getNickName() != null && !player.getNickName().equals(data.getPlayerName())) {
            data.setPlayerName(player.getNickName());
            changed = true;
        }
        if (player.getHeadImgId() != data.getHeadImgId()) {
            data.setHeadImgId(player.getHeadImgId());
            changed = true;
        }
        if (player.getHeadFrameId() != data.getHeadFrameId()) {
            data.setHeadFrameId(player.getHeadFrameId());
            changed = true;
        }
        if (changed) {
            autoSaveService.enqueueSave(data);
        }
    }

    /**
     * 注册时间是整条赛季时间线的锚点; 解析不到时直接失败, 绝不能用当前时间兜底落库造成永久漂移。
     */
    private long resolveTimelineOrigin(SimPlayerContext ctx, SeasonPlayerData data) {
        long origin = data.getTimelineOrigin();
        if (origin > 0) {
            return origin;
        }
        Player player = ctx.getPlayerController() == null ? null : ctx.getPlayerController().getPlayer();
        if (player == null || player.getCreateTime() <= 0) {
            player = corePlayerService.get(ctx.playerId());
        }
        if (player == null || player.getCreateTime() <= 0) {
            throw new IllegalStateException("无法确定玩家注册时间, 赛季时间线解析失败 playerId=" + ctx.playerId());
        }
        origin = player.getCreateTime() * 1000L;
        data.setTimelineOrigin(origin);
        return origin;
    }

    private int dailyKey(long now) {
        LocalDate date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();
        return date.getYear() * 10_000 + date.getMonthValue() * 100 + date.getDayOfMonth();
    }

    private void applyPendingSettlements(SimPlayerContext ctx, SeasonPlayerData data, long now) {
        if (data.getSeasonKey() == null) {
            return;
        }
        //高频协议入口都会经过这里, 按间隔节流避免每个请求都查一次库
        if (now - data.getLastPendingCheckTime() < PENDING_CHECK_INTERVAL_MILLIS) {
            return;
        }
        data.setLastPendingCheckTime(now);
        List<SeasonPendingSettlement> pending = seasonPlayerDao.findPendingSettlements(
                ctx.playerId(), data.getSeasonKey());
        if (pending == null || pending.isEmpty()) {
            return;
        }
        long coinBefore = data.getSeasonCoin();
        int applied = 0;
        List<String> handledIds = new ArrayList<>(pending.size());
        for (SeasonPendingSettlement settlement : pending) {
            handledIds.add(settlement.getId());
            if (!data.markMatchProcessed(settlement.getMatchId(), settlement.getHistoryLimit())) {
                continue;
            }
            applied++;
            long delta = settlement.getCoinDelta();
            if (delta > 0) {
                //被挑战方赢币同样受本人每日赢取上限的梯度削减
                SeasonMatchCfg cfg = configService.matchForDay(currentDay(data, now));
                if (cfg != null && data.getDailyWinAmount() >= cfg.getDailyWinLimit()) {
                    delta = SeasonPolicy.applyRatio(delta, SeasonPolicy.ratioFor(
                            data.getDailyWinAmount() - cfg.getDailyWinLimit(), cfg.getProfitRatio()));
                }
                economyService.addEarnedCoin(ctx, delta);
                data.setDailyWinAmount(Math.addExact(data.getDailyWinAmount(), delta));
                if (settlement.getRecord() != null) {
                    settlement.getRecord().setCoinChange(delta);
                    settlement.getRecord().setResult(Long.compare(delta, 0));
                }
            } else if (delta < 0) {
                long loss = Math.min(-delta, data.getSeasonCoin());
                data.setSeasonCoin(data.getSeasonCoin() - loss);
                data.setDailyLossAmount(Math.addExact(data.getDailyLossAmount(), loss));
                if (settlement.getRecord() != null) {
                    settlement.getRecord().setCoinChange(-loss);
                    settlement.getRecord().setResult(Long.compare(-loss, 0));
                }
            }
            data.addMatchRecord(settlement.getRecord(), settlement.getHistoryLimit());
        }
        //被动应下的币变动玩家看不到过程, 只有这条能解释"离线期间赛季币变了"
        log.info("赛季消费跨节点待结算 playerId={},seasonKey={},pending={},applied={},coin={}->{}",
                ctx.playerId(), data.getSeasonKey(), pending.size(), applied, coinBefore, data.getSeasonCoin());
        //数据先落库、再删除待结算记录; 两者都在落库 IO 线程 FIFO 执行, 崩溃时靠 processedMatchIds 幂等重放
        autoSaveService.enqueueSave(data);
        autoSaveService.enqueueTask(() -> seasonPlayerDao.deletePendingSettlements(handledIds));
    }

    private SeasonSnapshot snapshot(SeasonPlayerData data, long now) {
        return new SeasonSnapshot(data.getSeasonId(), data.seasonPhase(), data.getCycleIndex(),
                data.getStartTime(), data.getEndTime(), currentDay(data, now), data.getSeasonKey());
    }

    private int currentDay(SeasonPlayerData data, long now) {
        return SeasonTimeline.currentDay(data.getStartTime(), now);
    }

    /**
     * 结算上一赛季: 排名奖励 + 段位结算奖励通过邮件发放 (按 seasonKey+playerId 幂等);
     * 奖励中的赛季币部分不进邮件, 连同赛季末剩余赛季币的返还部分, 作为下一赛季的初始币直接带入。
     */
    private long settlePreviousSeason(SimPlayerContext ctx, SeasonPlayerData data) {
        if (data.getSeasonId() == 0 || data.seasonPhase() == null) {
            return 0;
        }
        Map<Integer, Long> rewards = new HashMap<>();
        //发奖名次必须实时, 不能用展示缓存
        int rank = rankingService.freshRankOf(data);
        SeasonRankingCfg ranking = configService.rankingReward(data.seasonPhase(), rank);
        if (ranking != null && ranking.getGetItem() != null) {
            ranking.getGetItem().forEach((id, count) -> rewards.merge(id, count, Long::sum));
        }
        SeasonTierCfg tier = configService.tiers(data.seasonPhase()).stream()
                .filter(cfg -> cfg.getId() == data.getTierId()).findFirst().orElse(null);
        if (tier != null && tier.getSettlementReward() != null) {
            tier.getSettlementReward().forEach((id, count) -> rewards.merge(id, count, Long::sum));
        }
        int currencyId = configService.currencyItemId();
        long initialCoin = Math.addExact(currencyId == 0 ? 0 : rewards.getOrDefault(currencyId, 0L),
                returnCoin(data.getSeasonCoin()));
        //结算快照 (含全部奖励): 跨季后玩家首次请求赛季信息时随 ResSeasonInfo 下发
        //须在 startSeason 清空 trialStars 之前采集
        SeasonSettlement settlement = new SeasonSettlement();
        settlement.setSeasonId(data.getSeasonId());
        settlement.setPhase(data.seasonPhase().ordinal() + 1);
        settlement.setCycleIndex(data.getCycleIndex());
        settlement.setRank(rank);
        settlement.setTierId(data.getTierId());
        settlement.setTotalEarnedCoin(data.getTotalEarnedCoin());
        settlement.setTotalTrialStars(data.getTrialStars().values().stream()
                .mapToInt(Integer::intValue).sum());
        settlement.setRewards(new HashMap<>(rewards));
        settlement.setInitialCoin(initialCoin);
        settlement.setSeasonBadge(tier == null ? 0 : tier.getSeasonBadge());
        data.setLastSettlement(settlement);
        //每赛季每人一次: 名次和段位直接决定发奖内容, 玩家申诉时靠这条还原
        log.info("赛季结算 playerId={},seasonKey={},rank={},tierId={},totalEarnedCoin={},leftCoin={},initialCoin={},rewards={}",
                ctx.playerId(), data.getSeasonKey(), rank, data.getTierId(),
                data.getTotalEarnedCoin(), data.getSeasonCoin(), initialCoin, rewards);
        //赛季徽章: 每赛季仅按最终段位授予唯一一枚勋章 (同 ranktype 一枚)
        if (tier != null && tier.getSeasonBadge() > 0 && ctx.getSimBaseData() != null) {
            ctx.getSimBaseData().activeMedalId(tier.getSeasonBadge());
            ctx.setLastSaveTime(0);
        }
        rewards.remove(currencyId);
        if (!rewards.isEmpty()) {
            List<Item> items = new ArrayList<>(rewards.size());
            rewards.forEach((id, count) -> items.add(new Item(id, count)));
            try {
                mailService.addMailIfAbsent(ctx.playerId(), "赛季结算奖励",
                        "上赛季结算排名第" + rank + "名，奖励已发放，请查收。", items,
                        AddType.ACTIVITY, "season-settle:" + data.getSeasonKey() + ":" + ctx.playerId());
            } catch (Exception e) {
                log.error("赛季结算奖励邮件发放失败 playerId={},seasonKey={}",
                        ctx.playerId(), data.getSeasonKey(), e);
            }
        }
        clearSeasonGems(ctx);
        return initialCoin;
    }

    /**
     * 赛季币返还: 赛季末剩余赛季币按配置比例 (百分比) 返还, 且不超过配置上限。
     */
    private long returnCoin(long seasonCoin) {
        int[] cfg = simConfigCacheService.getSeasonReturnMaxArr();
        if (seasonCoin <= 0 || cfg[0] <= 0) {
            return 0;
        }
        return Math.min(Math.multiplyExact(seasonCoin, cfg[0]) / 100, cfg[1]);
    }

    private void clearSeasonGems(SimPlayerContext ctx) {
        PlayerPack pack = playerPackService.getFromAllDB(ctx.playerId());
        if (pack == null) {
            return;
        }
        Map<Integer, Long> gems = new HashMap<>();
        configService.gems().forEach(cfg -> {
            long count = pack.getItemCount(cfg.getItemId());
            if (count > 0) {
                gems.put(cfg.getItemId(), count);
            }
        });
        if (gems.isEmpty()) {
            return;
        }
        Player player = ctx.getPlayerController() == null ? corePlayerService.get(ctx.playerId())
                : ctx.getPlayerController().getPlayer();
        var result = playerPackService.removeItems(player, gems, AddType.ACTIVITY,
                "season-gem-reset:" + ctx.getSeasonPlayerData().getSeasonKey());
        if (!result.success()) {
            log.warn("赛季宝石重置失败 playerId={},seasonKey={},code={}",
                    ctx.playerId(), ctx.getSeasonPlayerData().getSeasonKey(), result.code);
        }
    }
}
