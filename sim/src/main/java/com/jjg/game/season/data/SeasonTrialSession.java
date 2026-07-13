package com.jjg.game.season.data;

/**
 * 玩家当前进行中的试炼挑战窗口。挑战免费且可无限重试, 无超时;
 * 发起新挑战直接替换旧会话, 切季由 startSeason 清空。
 * <p>
 * progress 语义随条件类型: 12601=累计赢奖, 12602/12603/12604/12605=达标次数, 12606=单局最高赢奖。
 */
public class SeasonTrialSession {
    private int trialId;
    //窗口内已计入的局数
    private int spinCount;
    //当前进度值
    private long progress;
    private long startedAt;

    public int getTrialId() {
        return trialId;
    }

    public void setTrialId(int trialId) {
        this.trialId = trialId;
    }

    public int getSpinCount() {
        return spinCount;
    }

    public void setSpinCount(int spinCount) {
        this.spinCount = spinCount;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }
}
