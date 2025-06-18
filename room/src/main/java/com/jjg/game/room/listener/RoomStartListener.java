package com.jjg.game.room.listener;

/**
 * @author 11
 * @date 2025/6/17 13:28
 */
public interface RoomStartListener {
    /**
     * 获取支持的游戏类型
     * @return
     */
    int[] getGameTypes();

    /**
     * 启动
     */
    void start();

    /**
     * 结束
     */
    void shutdown();
}
