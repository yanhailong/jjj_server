package com.jjg.game.slots.game.mahjiongwin.data;

import com.jjg.game.slots.data.AwardLineInfo;

import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2025/8/1 17:31
 */
public class MahjiongWinAwardLineInfo extends AwardLineInfo {
    //中奖线的倍数
    private int baseTimes;
    //列 -> iconsId
    private Map<Integer, Set<Integer>> sameMap;

    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }

    public Map<Integer, Set<Integer>> getSameMap() {
        return sameMap;
    }

    public void setSameMap(Map<Integer, Set<Integer>> sameMap) {
        this.sameMap = sameMap;
    }
}
