package com.jjg.game.sim.data;

/**
 * 协作房间结算回执。保存在发起者任务文档中，生命周期独立于可领取后删除的任务条目。
 */
public class CoopSettlementReceipt {
    private int taskId;
    private int status;
    private long finishTime;

    public CoopSettlementReceipt() {
    }

    public CoopSettlementReceipt(int taskId, int status, long finishTime) {
        this.taskId = taskId;
        this.status = status;
        this.finishTime = finishTime;
    }

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

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }
}
