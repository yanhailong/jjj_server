package com.jjg.game.sim.service;

import com.jjg.game.core.base.condition.numeric.GameConditionEvent;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sim.data.SpinStatInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * slots 跨节点旋转统计到 core 条件事件的唯一转换边界。
 * <p>
 * 同一 spin 只创建一个不可变事件，供 sim 任务、联盟任务和赛季试炼同步复用，避免高频路径
 * 重复复制模式/图标集合。转换不访问数据库、Redis，也不切换线程。
 */
public final class SimConditionEventFactory {
    private static final Logger log = LoggerFactory.getLogger(SimConditionEventFactory.class);
    private static final AtomicBoolean missingGoldLogged = new AtomicBoolean();
    private static volatile int goldItemId;

    private SimConditionEventFactory() {
    }

    public static GameConditionEvent fromSpin(int gameType, int winTimes, int costPower,
                                              SpinStatInfo statInfo) {
        return fromSpin(gameType, winTimes, costPower, statInfo, resolveGoldItemId());
    }

    /** 测试/无货币条件调用可显式传 0，避免依赖尚未初始化的 Item 配置。 */
    public static GameConditionEvent fromSpin(int gameType, int winTimes, int costPower,
                                              SpinStatInfo statInfo, int goldItemId) {
        long bet = statInfo == null ? costPower : statInfo.getBet();
        long win = statInfo == null ? 0 : statInfo.getWin();
        return new GameConditionEvent(gameType, gameType, 0, goldItemId, goldItemId,
                bet, win, winTimes, costPower > 0 || bet > 0, true,
                statInfo == null ? 0 : statInfo.getBigShowId(), jackpotType(statInfo),
                statInfo != null && statInfo.getRemainFreeCount() > 0 ? 1 : 0,
                statInfo == null || statInfo.getSpecialModes() == null
                        ? Set.of() : Set.copyOf(statInfo.getSpecialModes()),
                statInfo == null ? List.of() : statInfo.getIcons(),
                win > 0 ? Map.of(goldItemId, win) : Map.of());
    }

    /**
     * 非 spin 游戏结算和旧跨节点协议的统一适配入口，同样使用金币配置未就绪时的安全降级策略。
     */
    public static GameConditionEvent fromGameResult(int gameType, long bet, long win, long multiple) {
        int goldItemId = resolveGoldItemId();
        return new GameConditionEvent(gameType, gameType, 0, goldItemId, goldItemId,
                bet, win, multiple, true, true, 0, 0, 0,
                Set.of(), List.of(), win > 0 ? Map.of(goldItemId, win) : Map.of());
    }

    private static int jackpotType(SpinStatInfo statInfo) {
        if (statInfo == null) return 0;
        if (statInfo.getMini() > 0) return 1;
        if (statInfo.getMinor() > 0) return 2;
        if (statInfo.getMajor() > 0) return 3;
        if (statInfo.getGrand() > 0) return 4;
        return 0;
    }

    private static int resolveGoldItemId() {
        int cached = goldItemId;
        if (cached > 0) {
            return cached;
        }
        try {
            cached = ItemUtils.getGoldItemId();
            if (cached > 0) {
                goldItemId = cached;
                return cached;
            }
        } catch (RuntimeException e) {
            if (missingGoldLogged.compareAndSet(false, true)) {
                log.error("构造条件事件时金币道具配置尚未就绪，货币过滤条件本次不推进", e);
            }
        }
        return 0;
    }
}
