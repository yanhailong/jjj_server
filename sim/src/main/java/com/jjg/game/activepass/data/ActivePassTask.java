package com.jjg.game.activepass.data;

/** 当期任务进度；不同日期/期数不复用计数器。 */
public class ActivePassTask {
    private int taskId;
    private long progress;
    private boolean claimed;

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public long getProgress() { return progress; }
    public void setProgress(long progress) { this.progress = progress; }
    public boolean isClaimed() { return claimed; }
    public void setClaimed(boolean claimed) { this.claimed = claimed; }
}
