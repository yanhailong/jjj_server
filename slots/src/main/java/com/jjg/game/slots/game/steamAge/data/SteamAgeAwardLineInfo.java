package com.jjg.game.slots.game.steamAge.data;

import com.jjg.game.slots.data.AwardLineInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:31
 */
public class SteamAgeAwardLineInfo extends AwardLineInfo {
    //连续倍数
    private int baseTimes;
    //线倍数
    private int lineTimes;
    //总倍数 线倍数*连续倍数
    private int totalTimes;
    //相同元素的坐标id
    private Set<Integer> sameIconSet;
    //元素id
    private int sameIcon;

    public int getSameCount() {
        return sameIconSet == null ? 0 : sameIconSet.size();
    }

    public int getTotalTimes() {
        return totalTimes;
    }

    public void setTotalTimes(int totalTimes) {
        this.totalTimes = totalTimes;
    }

    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }

    public int getLineTimes() {
        return lineTimes;
    }

    public void setLineTimes(int lineTimes) {
        this.lineTimes = lineTimes;
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

    public void addSameIconIndexSet(int icon) {

        if (sameIconSet == null) {
            sameIconSet = new HashSet<>();
        }
        sameIconSet.add(icon);
    }
}
