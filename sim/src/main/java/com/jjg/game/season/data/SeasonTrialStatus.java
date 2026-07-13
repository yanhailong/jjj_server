package com.jjg.game.season.data;

import com.jjg.game.season.config.SeasonTrialDef;

/**
 * 试炼关卡在某玩家视角下的状态 (列表展示用)。
 */
public class SeasonTrialStatus {
    private SeasonTrialDef def;
    //已达成最高星级 (0=未通关)
    private int stars;
    private boolean unlocked;
    //是否为当前进行中的挑战
    private boolean active;
    //进行中挑战已用局数 (被动型/非进行中为 0)
    private int spinCount;
    //进行中挑战的当前进度 / 被动型的当前累计值
    private long progress;

    public SeasonTrialDef getDef() {
        return def;
    }

    public void setDef(SeasonTrialDef def) {
        this.def = def;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
}
