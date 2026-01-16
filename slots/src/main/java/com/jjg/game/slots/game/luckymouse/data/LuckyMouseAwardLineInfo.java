package com.jjg.game.slots.game.luckymouse.data;

import com.jjg.game.slots.data.AwardLineInfo;

public class LuckyMouseAwardLineInfo extends AwardLineInfo {

    private int id;
    private int baseTimes;
    private int iconId;
    //这条线上相同的个数
    protected int sameCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public int getSameCount() {
        return sameCount;
    }

    public void setSameCount(int sameCount) {
        this.sameCount = sameCount;
    }
}
