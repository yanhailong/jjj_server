package com.jjg.game.season.service;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.dao.PlayerRechargeFlowDao;
import com.jjg.game.core.logger.TaskLogger;
import com.jjg.game.sampledata.bean.TaskCfg;
import com.jjg.game.season.config.SeasonTrialDef;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.data.SeasonTrialResult;
import com.jjg.game.season.data.SeasonTrialSession;
import com.jjg.game.season.data.SeasonTrialStatus;
import com.jjg.game.season.model.SeasonPhase;
import com.jjg.game.season.model.SeasonSnapshot;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.service.SimAutoSaveService;
import com.jjg.game.sim.service.SimPackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新手赛季试炼关卡 (task 表 taskType=6)。
 * <p>
 * 挑战型条件 (12601-12606) 为 "N局窗口内达成目标": 发起挑战建立会话,
 * 会话状态全程驻留 {@link SeasonPlayerData} 内存 (旋转热路径零查库/零Redis),
 * 窗口结束或满星提前结算, 按 1/2/3 星达标线发奖 (奖励自动发放, 只发新突破的星级)。
 * 挑战免费且可无限重试, 发起新挑战直接替换旧会话。
 * 被动型条件 (11002 累计充值) 无挑战会话, 开面板时按赛季时间窗查充值流水惰性判定。
 */
@Service
public class SeasonTrialService {
    private static final Logger log = LoggerFactory.getLogger(SeasonTrialService.class);

    //条件类型 (condition 表 id)
    private static final int COND_TOTAL_WIN = 12601;
    private static final int COND_MODE_TRIGGER = 12602;
    private static final int COND_MULTIPLE = 12603;
    private static final int COND_WIN_COUNT = 12604;
    private static final int COND_ICON_COUNT = 12605;
    private static final int COND_SINGLE_WIN = 12606;

    private final SeasonTrialConfigService trialConfigService;
    private final SeasonConfigService configService;
    private final SeasonEconomyService economyService;
    private final SimPackService simPackService;
    private final SimAutoSaveService autoSaveService;
    private final PlayerRechargeFlowDao playerRechargeFlowDao;
    private final TaskLogger taskLogger;

    public SeasonTrialService(SeasonTrialConfigService trialConfigService, SeasonConfigService configService,
                              SeasonEconomyService economyService, SimPackService simPackService,
                              SimAutoSaveService autoSaveService, PlayerRechargeFlowDao playerRechargeFlowDao,
                              TaskLogger taskLogger) {
        this.trialConfigService = trialConfigService;
        this.configService = configService;
        this.economyService = economyService;
        this.simPackService = simPackService;
        this.autoSaveService = autoSaveService;
        this.playerRechargeFlowDao = playerRechargeFlowDao;
        this.taskLogger = taskLogger;
    }

    /**
     * 关卡静态定义 (协议组装用)
     */
    public SeasonTrialDef trialDef(int trialId) {
        return trialConfigService.trial(trialId);
    }

    // =====================================================================
    // 列表
    // =====================================================================

    /**
     * 试炼关卡列表 (仅新手赛季)。被动型关卡在此惰性判定并自动发奖 (充值无事件钩子)。
     */
    public List<SeasonTrialStatus> list(SimPlayerContext ctx, SeasonSnapshot snapshot, long now) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (data == null || snapshot.phase() != SeasonPhase.NOVICE) {
            return List.of();
        }
        List<SeasonTrialDef> defs = trialConfigService.trials();
        SeasonTrialSession session = data.getActiveTrial();
        List<SeasonTrialStatus> statuses = new ArrayList<>(defs.size());
        for (SeasonTrialDef def : defs) {
            SeasonTrialStatus status = new SeasonTrialStatus();
            status.setDef(def);
            status.setUnlocked(unlocked(def, snapshot.day(), data, defs));
            if (def.passive() && status.isUnlocked()) {
                evaluatePassive(ctx, data, def, now, status);
            } else if (session != null && session.getTrialId() == def.trialId()) {
                status.setActive(true);
                status.setSpinCount(session.getSpinCount());
                status.setProgress(session.getProgress());
            }
            status.setStars(starsOf(data, def.trialId()));
            statuses.add(status);
        }
        return statuses;
    }

    /**
     * 关卡解锁: 到达所属天数, 且前一天的关卡全部通关 (≥1星)。
     */
    static boolean unlocked(SeasonTrialDef def, int day, SeasonPlayerData data, List<SeasonTrialDef> defs) {
        if (def.day() > day) {
            return false;
        }
        if (def.day() <= 1) {
            return true;
        }
        for (SeasonTrialDef other : defs) {
            if (other.day() == def.day() - 1 && starsOf(data, other.trialId()) < 1) {
                return false;
            }
        }
        return true;
    }

    // =====================================================================
    // 发起挑战
    // =====================================================================

    /**
     * 发起试炼挑战: 免费无限次, 已有进行中的挑战被直接替换 (等价于免费重试)。
     */
    public CommonResult<SeasonTrialSession> challenge(SimPlayerContext ctx, int trialId,
                                                      SeasonSnapshot snapshot, long now) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (data == null || snapshot.phase() != SeasonPhase.NOVICE) {
            return fail(Code.NOT_UNLOCKED, ctx, trialId, "非新手赛季");
        }
        SeasonTrialDef def = trialConfigService.trial(trialId);
        if (def == null) {
            return fail(Code.NOT_FOUND, ctx, trialId, "关卡不存在");
        }
        if (def.passive()) {
            return fail(Code.PARAM_ERROR, ctx, trialId, "被动关卡不可挑战");
        }
        if (!unlocked(def, snapshot.day(), data, trialConfigService.trials())) {
            return fail(Code.NOT_UNLOCKED, ctx, trialId, "关卡未解锁");
        }
        if (starsOf(data, trialId) >= SeasonTrialDef.STAR_COUNT) {
            return fail(Code.REPEAT_OP, ctx, trialId, "关卡已满星");
        }
        SeasonTrialSession old = data.getActiveTrial();
        if (old != null) {
            log.info("玩家[{}]发起试炼挑战替换旧会话 oldTrialId={},oldSpins={}",
                    ctx.playerId(), old.getTrialId(), old.getSpinCount());
        }
        SeasonTrialSession session = new SeasonTrialSession();
        session.setTrialId(trialId);
        session.setStartedAt(now);
        data.setActiveTrial(session);
        autoSaveService.enqueueSave(data);
        log.info("玩家[{}]发起试炼挑战 trialId={},window={}", ctx.playerId(), trialId, def.windowSpins());
        return new CommonResult<>(Code.SUCCESS, session);
    }

    // =====================================================================
    // 旋转推进 (热路径)
    // =====================================================================

    /**
     * slots 旋转联动: 无进行中挑战时仅一次空判直接返回。窗口结束或提前满星时结算。
     *
     * @return 发生结算时返回结果, 否则 null
     */
    public SeasonTrialResult onSpin(SimPlayerContext ctx, int gameType, SpinStatInfo statInfo) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonTrialSession session = data == null ? null : data.getActiveTrial();
        if (session == null || statInfo == null) {
            return null;
        }
        SeasonTrialDef def = trialConfigService.trial(session.getTrialId());
        if (def == null || def.passive()) {
            //配置热更导致会话失效: 丢弃会话
            log.warn("试炼会话对应配置失效, 丢弃 playerId={},trialId={}", ctx.playerId(), session.getTrialId());
            data.setActiveTrial(null);
            autoSaveService.enqueueSave(data);
            return null;
        }
        if (def.gameType() != 0 && def.gameType() != gameType) {
            //其他机台的旋转不计入窗口
            return null;
        }
        session.setSpinCount(session.getSpinCount() + 1);
        session.setProgress(advance(def, session.getProgress(), statInfo));
        int achieved = def.starsOf(session.getProgress());
        if (achieved >= SeasonTrialDef.STAR_COUNT || session.getSpinCount() >= def.windowSpins()) {
            return settle(ctx, data, def, session, achieved);
        }
        return null;
    }

    /**
     * 按条件类型推进进度值。
     */
    static long advance(SeasonTrialDef def, long progress, SpinStatInfo statInfo) {
        long win = Math.max(0, statInfo.getWin());
        return switch (def.conditionId()) {
            case COND_TOTAL_WIN -> progress + win;
            case COND_MODE_TRIGGER -> progress + (containsMode(statInfo, (int) def.param()) ? 1 : 0);
            case COND_MULTIPLE -> progress + (statInfo.getMultiple() >= def.param() ? 1 : 0);
            case COND_WIN_COUNT -> progress + (win > 0 ? 1 : 0);
            case COND_ICON_COUNT -> progress + countIcons(statInfo.getIcons(), (int) def.param());
            case COND_SINGLE_WIN -> Math.max(progress, win);
            default -> progress;
        };
    }

    private static boolean containsMode(SpinStatInfo statInfo, int modeId) {
        return statInfo.getSpecialModes() != null && statInfo.getSpecialModes().contains(modeId);
    }

    private static int countIcons(List<Integer> icons, int elementId) {
        if (icons == null) {
            return 0;
        }
        int count = 0;
        for (Integer icon : icons) {
            if (icon != null && icon == elementId) {
                count++;
            }
        }
        return count;
    }

    // =====================================================================
    // 结算 / 发奖
    // =====================================================================

    private SeasonTrialResult settle(SimPlayerContext ctx, SeasonPlayerData data,
                                     SeasonTrialDef def, SeasonTrialSession session, int achieved) {
        int prevBest = starsOf(data, def.trialId());
        Map<Integer, Long> rewards = grantStarRewards(ctx, def, prevBest, achieved);
        if (achieved > prevBest) {
            data.getTrialStars().put(def.trialId(), achieved);
        }
        data.setActiveTrial(null);
        autoSaveService.enqueueSave(data);
        log.info("玩家[{}]试炼挑战结算 trialId={},spins={},progress={},achieved={},prevBest={}",
                ctx.playerId(), def.trialId(), session.getSpinCount(), session.getProgress(), achieved, prevBest);

        SeasonTrialResult result = new SeasonTrialResult();
        result.setTrialId(def.trialId());
        result.setAchievedStars(achieved);
        result.setBestStars(Math.max(prevBest, achieved));
        result.setRewards(rewards);
        result.setSpinCount(session.getSpinCount());
        result.setProgress(session.getProgress());
        result.setSeasonCoin(data.getSeasonCoin());
        return result;
    }

    /**
     * 被动型 (累计充值) 惰性判定: 满星后不再查库; 达成新星级时就地发奖落库,
     * 并把结算结果挂到 status 上, 供上层下发 NotifySeasonTrialResult。
     */
    private void evaluatePassive(SimPlayerContext ctx, SeasonPlayerData data, SeasonTrialDef def,
                                 long now, SeasonTrialStatus status) {
        int prevBest = starsOf(data, def.trialId());
        if (prevBest >= SeasonTrialDef.STAR_COUNT) {
            status.setProgress(def.topTarget());
            return;
        }
        long progress = playerRechargeFlowDao.sumAmountByPlayerIdAndTimeRange(
                ctx.playerId(), def.rechargeChannel(), data.getStartTime(), now).longValue();
        status.setProgress(progress);
        int achieved = def.starsOf(progress);
        if (achieved > prevBest) {
            Map<Integer, Long> rewards = grantStarRewards(ctx, def, prevBest, achieved);
            data.getTrialStars().put(def.trialId(), achieved);
            autoSaveService.enqueueSave(data);
            log.info("玩家[{}]试炼被动关卡达成 trialId={},progress={},achieved={},prevBest={}",
                    ctx.playerId(), def.trialId(), progress, achieved, prevBest);
            SeasonTrialResult result = new SeasonTrialResult();
            result.setTrialId(def.trialId());
            result.setAchievedStars(achieved);
            result.setBestStars(achieved);
            result.setRewards(rewards);
            result.setProgress(progress);
            result.setSeasonCoin(data.getSeasonCoin());
            status.setPassiveResult(result);
        }
    }

    /**
     * 发放 (fromExclusive, toInclusive] 星级的任务奖励; 赛季币走赛季经济入口, 其余进背包。
     *
     * @return 本次发放的道具合并结果
     */
    private Map<Integer, Long> grantStarRewards(SimPlayerContext ctx, SeasonTrialDef def,
                                                int fromExclusive, int toInclusive) {
        if (toInclusive <= fromExclusive) {
            return Map.of();
        }
        Map<Integer, Long> total = new HashMap<>();
        for (int star = fromExclusive + 1; star <= toInclusive; star++) {
            TaskCfg cfg = def.starTasks().get(star - 1);
            taskLogger.completeTask(ctx.playerId(), cfg.getId());
            List<Item> logItems = null;
            if (cfg.getGetItem() != null && !cfg.getGetItem().isEmpty()) {
                grant(ctx, cfg.getGetItem(), cfg.getId());
                cfg.getGetItem().forEach((id, count) -> total.merge(id, count, Long::sum));
                logItems = toItemList(cfg.getGetItem());
            }
            taskLogger.receiveTaskAward(ctx.playerId(), cfg.getId(), logItems,
                    cfg.getIntegralNum(), TaskConstant.TaskStatus.STATUS_REWARDED);
        }
        return total;
    }

    private void grant(SimPlayerContext ctx, Map<Integer, Long> items, int taskId) {
        Map<Integer, Long> packItems = new HashMap<>(items);
        int currencyId = configService.currencyItemId();
        Long coin = currencyId == 0 ? null : packItems.remove(currencyId);
        if (coin != null && coin > 0) {
            economyService.addEarnedCoin(ctx, coin);
        }
        if (!packItems.isEmpty()) {
            var result = simPackService.addItems(ctx, packItems, AddType.TASKAWARD,
                    "seasonTrial:" + taskId, true);
            if (result == null || !result.success()) {
                log.warn("试炼奖励发放失败 playerId={},taskId={},code={}",
                        ctx.playerId(), taskId, result == null ? Code.EXCEPTION : result.code);
            }
        }
    }

    private static List<Item> toItemList(Map<Integer, Long> items) {
        List<Item> list = new ArrayList<>(items.size());
        items.forEach((id, count) -> list.add(new Item(id, count)));
        return list;
    }

    private static int starsOf(SeasonPlayerData data, int trialId) {
        return data.getTrialStars().getOrDefault(trialId, 0);
    }

    private CommonResult<SeasonTrialSession> fail(int code, SimPlayerContext ctx, int trialId, String message) {
        log.warn("发起试炼挑战失败 playerId={},trialId={},code={},detail={}", ctx.playerId(), trialId, code, message);
        return new CommonResult<>(code);
    }
}
