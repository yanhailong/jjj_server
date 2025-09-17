package com.jjg.game.core.base.gameevent;

import com.jjg.game.core.constant.RechargeType;
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
        // 充值id
        private int id;
        // 充值类型
        private RechargeType type;

        public PlayerRechargeEvent(Player player,int id, RechargeType type) {
            super(player, EGameEventType.RECHARGE, null, null);
            this.id = id;
            this.type = type;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public RechargeType getType() {
            return type;
        }

        public void setType(RechargeType type) {
            this.type = type;
        }
    }
}
