package com.jjg.game.core.base.gameevent;

import com.jjg.game.core.constant.RechargeType;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;

/**
 * 玩家事件分组
 *
 * @author 2CL
 */
public class PlayerEventCategory {

    /**
     * 玩家有效流水发生事件
     */
    public static class PlayerEffectiveFlowingEvent extends PlayerEvent {
        // 游戏配置ID
        private int gameCfgId;

        public PlayerEffectiveFlowingEvent(
                Player player, int gameCfgId, Object eventChangeValue, Object newlyValue) {
            super(player, EGameEventType.EFFECTIVE_FLOWING, eventChangeValue, newlyValue);
            this.gameCfgId = gameCfgId;
        }

        public int getGameCfgId() {
            return gameCfgId;
        }

        public void setGameCfgId(int gameCfgId) {
            this.gameCfgId = gameCfgId;
        }
    }

    /**
     * 玩家充值事件
     */
    public static class PlayerRechargeEvent extends PlayerEvent {
        // 充值订单
        private Order order;

        public PlayerRechargeEvent(Player player,Order order) {
            super(player, EGameEventType.RECHARGE, null, null);
            this.order = order;
        }

        public Order getOrder() {
            return order;
        }

        public void setOrder(Order order) {
            this.order = order;
        }
    }
}
