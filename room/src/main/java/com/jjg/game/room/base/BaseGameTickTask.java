package com.jjg.game.room.base;

/**
 * 游戏tick任务回调
 *
 * @author 2CL
 */
public abstract class BaseGameTickTask {

    // tick任务类型
    private ETickTaskType tickTaskType;
    // 任务触发间隔
    private final int taskInterval;

    public enum ETickTaskType {
        // 玩家在游戏中无操作检查
        PLAYER_NO_OPERATE_CHECK,
        // 玩家数据回存检查
        ROOM_PLAYER_SAVE_CHECK,
    }

    public BaseGameTickTask(int taskInterval) {
        this.taskInterval = taskInterval;
    }

    public void setTickTaskType(ETickTaskType tickTaskType) {
        this.tickTaskType = tickTaskType;
    }

    public ETickTaskType getTickTaskType() {
        return tickTaskType;
    }

    public int getTaskInterval() {
        return taskInterval;
    }

    /**
     * 运行任务
     *
     * @param triggeredTimestamp 触发任务时的时间戳
     */
    public abstract void run(long triggeredTimestamp);
}
