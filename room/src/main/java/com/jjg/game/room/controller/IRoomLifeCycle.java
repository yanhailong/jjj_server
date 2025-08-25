package com.jjg.game.room.controller;

import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;

/**
 * 房间生命周期
 *
 * @author 2CL
 */
public interface IRoomLifeCycle {

    /**
     * 房间游戏开始
     */
    void startGame();

    /**
     * 房间初始化,房间被创建后的初始化逻辑,房间内游戏的逻辑初始化
     */
    void initial();

    /**
     * 房间就绪
     */
    void roomReady();

    /**
     * 房间更新,房间内的定时游戏逻辑在此处更新,每200ms Tick一次,需要控制好tick时间，不能小于最耗时的逻辑代码的时间
     */
    void roomTick();

    /**
     * 游戏玩家托管逻辑，游戏中如果默认的出牌超时后，将进入自动托管逻辑，由每个游戏具体实现
     */
    void hosting();

    /**
     * 断线重连
     */
    void reconnect();

    /**
     * 玩家主动请求退出房间
     */
    <R extends Room> CommonResult<R> onPlayerLeaveRoom(PlayerController playerController);

    /**
     * 房间解散时调用
     * 调用路径 (房间游戏结束) -> (RoomManager.disbandRoom) -> (RoomController.disbandRoom) -> (GameController.disbandRoom)
     */
    void disbandRoom();

    /**
     * 游戏结束
     */
    void gameOver();

    /**
     * 停止游戏，销毁房间或者停止房间定时器和状态机时调用
     */
    void stopGame();

    /**
     * 暂停游戏
     */
    void pauseGame();

    /**
     * 继续游戏
     */
    void continueGame();

    /**
     * 销毁房间前
     */
    default void beforeDestroyRoom() {
    }
}
