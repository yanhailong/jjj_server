package com.jjg.game.sim.data;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;

import java.util.List;

/**
 * 多人协作任务规则 (由 task.xlsx 的 taskConditionId 与人数/时限字段解析而来)。
 * <p>
 * 条件格式 (condition 表 12501): {@code 12501_游戏id(0=任意)_下注具体次数_游戏模式id_触发模式次数}。
 * 下注具体次数即团队总"血条"(Spin 总目标), 开始时按成员数平分; 触发模式次数为全队共享累计目标。
 *
 * @param taskId          任务配置id
 * @param conditionId     条件配置id (condition 表)
 * @param gameType        限定游戏 (0=任意已解锁游戏)
 * @param spinBudget      团队 Spin 总目标 (血条总量)
 * @param modeId          特殊事件的游戏模式 (SpecialMode.type)
 * @param modeCount       特殊事件目标触发次数 (全队共享累计)
 * @param minMembers      开始游戏所需最少总人数 (含房主, 最低 1)
 * @param maxMembers      房间总人数上限 (含房主)
 * @param durationMinutes 任务时限(分, 0=不限); 开始后超时未完成判定失败, 兜底成员挂机导致房间悬挂
 * @param condition       已在配置加载阶段校验的通用条件，slots 热路径直接复用
 * @author 11
 * @date 2026/7/6
 */
public record CoopTaskRule(int taskId, int conditionId, int gameType, int spinBudget,
                           int modeId, long modeCount, int minMembers, int maxMembers,
                           int durationMinutes, PreparedCondition condition) {

    /**
     * 保留旧构造签名，兼容现有测试和节点滚动升级期间的本地调用；生产配置使用完整构造器。
     */
    public CoopTaskRule(int taskId, int conditionId, int gameType, int spinBudget,
                        int modeId, long modeCount, int minMembers, int maxMembers,
                        int durationMinutes) {
        this(taskId, conditionId, gameType, spinBudget, modeId, modeCount, minMembers, maxMembers,
                durationMinutes, prepareLegacy(conditionId, gameType, spinBudget, modeId, modeCount));
    }

    private static PreparedCondition prepareLegacy(int conditionId, int gameType, int spinBudget,
                                                     int modeId, long target) {
        List<Long> values = switch (conditionId) {
            case 12501, 12503 -> List.of((long) conditionId, (long) gameType, (long) spinBudget,
                    (long) modeId, target);
            case 12502, 12504 -> List.of((long) conditionId, (long) gameType, (long) spinBudget, target);
            default -> throw new IllegalArgumentException("unsupported coop condition id: " + conditionId);
        };
        return ConditionRuleRegistry.standard().prepare(ConditionSpec.from(values));
    }
}
