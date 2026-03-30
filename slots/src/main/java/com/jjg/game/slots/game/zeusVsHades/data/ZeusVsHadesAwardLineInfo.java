package com.jjg.game.slots.game.zeusVsHades.data;

import com.jjg.game.slots.data.AwardLineInfo;

import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:31
 */
public class ZeusVsHadesAwardLineInfo extends AwardLineInfo {
    //中奖线的倍数
    private int baseTimes;
    //相同元素的坐标id （线上中奖的元素）
    private Set<Integer> sameIconSet;
    //元素id
    private int sameIcon;
    private int vsTime;
    //总倍数 = baseTimes * vsTime
    private int totalTime;
    /**
     * 线id
     */
    private int lineId;

    public int getVsTime() {
        return vsTime;
    }

    public void setVsTime(int vsTime) {
        this.vsTime = vsTime;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(int totalTime) {
        this.totalTime = totalTime;
    }

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }


    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }

    public Set<Integer> getSameIconSet() {
        return sameIconSet;
    }

    public void setSameIconSet(Set<Integer> sameIconSet) {
        this.sameIconSet = sameIconSet;
    }

    public int getSameIcon() {
        return sameIcon;
    }

    public void setSameIcon(int sameIcon) {
        this.sameIcon = sameIcon;
    }
}
