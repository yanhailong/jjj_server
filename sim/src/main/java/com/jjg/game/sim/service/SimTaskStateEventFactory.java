package com.jjg.game.sim.service;

import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.condition.numeric.StateConditionEvent;
import com.jjg.game.core.data.Player;

/**
 * sim 任务可实时读取的玩家状态适配器。
 * <p>
 * 开服天数、手机绑定和活动剩余次数目前没有位于 sim/core Player 快照中的可靠数据源；在对应
 * 数据源接入前由配置加载期拒绝，避免任务成功入链后永久无法完成。新增状态源时只需在本类补充映射。
 */
final class SimTaskStateEventFactory {
    private SimTaskStateEventFactory() {
    }

    static boolean supports(PreparedCondition condition) {
        return condition != null && switch (condition.spec().id()) {
            case 1, 3 -> true;
            default -> false;
        };
    }

    static StateConditionEvent from(Player player, PreparedCondition condition) {
        if (player == null || condition == null) {
            return null;
        }
        return switch (condition.spec().id()) {
            case 1 -> new StateConditionEvent(StateConditionEvent.Type.PLAYER_LEVEL, 0,
                    player.getLevel(), 0);
            case 3 -> new StateConditionEvent(StateConditionEvent.Type.VIP_LEVEL, 0,
                    player.getVipLevel(), 0);
            default -> null;
        };
    }
}
