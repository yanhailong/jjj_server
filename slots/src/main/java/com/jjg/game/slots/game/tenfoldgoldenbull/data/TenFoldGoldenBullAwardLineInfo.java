package com.jjg.game.slots.game.tenfoldgoldenbull.data;

import com.jjg.game.slots.data.AwardLineInfo;

/**
 * @author lihaocao
 * @date 2025/12/2 17:31
 */
public class TenFoldGoldenBullAwardLineInfo extends AwardLineInfo {
    //线的id
    protected int id;
    //这条线的基础倍数
    protected int baseTimes;
    //图标id
    protected int iconId;
    //这条线上相同的个数
    protected int sameCount;

    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
