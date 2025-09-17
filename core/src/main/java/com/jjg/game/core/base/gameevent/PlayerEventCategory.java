package com.jjg.game.core.base.gameevent;

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
            super(player, EGameEventType.PLAYER_BET, eventChangeValue, newlyValue);
            this.gameCfgId = gameCfgId;
        }

        public int getGameCfgId() {
            return gameCfgId;
        }

        public void setGameCfgId(int gameCfgId) {
            this.gameCfgId = gameCfgId;
        }
    }
}
