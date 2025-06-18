package com.jjg.game.dollarexpress.data;

import com.jjg.game.core.data.PlayerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 玩家游戏数据
 * @author 11
 * @date 2025/6/10 18:07
 */
public class PlayerGameData {
    private static final Logger log = LoggerFactory.getLogger(PlayerGameData.class);

    private PlayerController playerController;
    //游戏类型
    private int gameType;

    //场次配置id
    private int wareId;


    public PlayerGameData(PlayerController playerController) {
        this.playerController = playerController;
    }

    public PlayerController getPlayerController() {
        return playerController;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public void setWareId(int wareId) {
        this.wareId = wareId;
    }

    public int getWareId(){
        return this.wareId;
    }

    public long playerId(){
        return playerController.playerId();
    }
}
