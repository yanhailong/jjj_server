package com.jjg.game.sim.data;

/**
 * 玩家已领取的单条多人协作任务 (嵌入 {@link SimCoopTaskData})。
 *
 * @author 11
 * @date 2026/7/6
 */
public class SimCoopTaskEntry {
    //任务配置id (task.xlsx taskType=4)
    private int taskId;
    //状态 {@link com.jjg.game.sim.constant.CoopTaskConst.TaskStatus}
    private int status;
    //已创建的协作房间id (未创建为 0)
    private long roomId;
    //创建房间时选择的游戏
    private int gameType;
    private long claimTime;
    //结算时间 (成功/失败)
    private long finishTime;

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public long getClaimTime() {
        return claimTime;
    }

    public void setClaimTime(long claimTime) {
        this.claimTime = claimTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }
}
