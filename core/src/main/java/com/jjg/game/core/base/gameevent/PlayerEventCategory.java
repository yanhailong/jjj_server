package com.jjg.game.core.base.gameevent;

import com.jjg.game.core.data.Player;

/**
 * 玩家事件分组
 *
 * @author 2CL
 */
public class PlayerEventCategory {

    public static class PlayerEffectiveFlowingEvent extends PlayerEvent {
        // 游戏类型
        private int gameType;
        // 游戏配置ID
        private int gameCfgId;

        public PlayerEffectiveFlowingEvent(
            Player player, int gameType, Object eventChangeValue, Object newlyValue) {
            super(player, EGameEventType.PLAYER_BET, eventChangeValue, newlyValue);
            this.gameType = gameType;
        }

        public int getGameType() {
            return gameType;
        }

        public void setGameType(int gameType) {
            this.gameType = gameType;
        }

        public int getGameCfgId() {
            return gameCfgId;
        }

        public void setGameCfgId(int gameCfgId) {
            this.gameCfgId = gameCfgId;
        }
    }
}
