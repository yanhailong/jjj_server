package com.jjg.game.room.data.room;

/**
 * @author lm
 * @date 2025/7/26 11:23
 */
public class PokerPlayerGameData {
    /**
     * 玩家上一次在房间主动操作并与服务端有交互的时间
     */
    private long playerLatestOperateTime;
    /**
     * 加入房间的时间
     */
    private long joinTime;

    /**
     * 是否初始化完成
     */
    private boolean isInit;

    public long getPlayerLatestOperateTime() {
        return playerLatestOperateTime;
    }

    public void setPlayerLatestOperateTime(long playerLatestOperateTime) {
        this.playerLatestOperateTime = playerLatestOperateTime;
    }

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        isInit = init;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }
}
