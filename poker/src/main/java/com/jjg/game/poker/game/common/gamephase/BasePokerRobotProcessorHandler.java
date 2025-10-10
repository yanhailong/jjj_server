package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.room.data.robot.GameRobotPlayer;

/**
 * @author lm
 * @date 2025/9/30 11:05
 */
public abstract class BasePokerRobotProcessorHandler<T extends BasePokerGameDataVo> extends BaseHandler<String> {
    //机器人
    private final GameRobotPlayer gameRobotPlayer;
    //操作类型
    private final int type;
    //游戏控制器
    private final BasePokerGameController<T> gameController;
    //结算时获得金币
    private final long getValue;

    public BasePokerRobotProcessorHandler(GameRobotPlayer gameRobotPlayer, int type, BasePokerGameController<T> gameController, long getValue) {
        this.gameRobotPlayer = gameRobotPlayer;
        this.type = type;
        this.gameController = gameController;
        this.getValue = getValue;
    }

    public BasePokerRobotProcessorHandler(GameRobotPlayer gameRobotPlayer, int type, BasePokerGameController<T> gameController) {
        this.gameRobotPlayer = gameRobotPlayer;
        this.type = type;
        this.gameController = gameController;
        this.getValue = 0;
    }

    public long getPlayerId() {
        return gameRobotPlayer.getId();
    }

    public long getGetValue() {
        return getValue;
    }

    public GameRobotPlayer getGameRobotPlayer() {
        return gameRobotPlayer;
    }

    public BasePokerGameController<T> getGameController() {
        return gameController;
    }

    public int getType() {
        return type;
    }
}
