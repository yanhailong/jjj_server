package com.jjg.game.slots.game.mahjiongwin.data;

import com.jjg.game.slots.data.AwardLineInfo;

import java.util.Set;

/**
 * @author 11
 * @date 2025/8/1 17:31
 */
public class MahjiongWinAwardLineInfo extends AwardLineInfo {
    //中奖线的倍数
    private int baseTimes;
    //相同元素的坐标id
    private Set<Integer> sameIconSet;

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
}
