package com.jjg.game.slots.game.findgoldcity.data;

import com.jjg.game.slots.data.FullAwardLineInfo;

import java.util.Map;

/**
 * @author lm
 */
public class FindGoldCityAwardLineInfo extends FullAwardLineInfo {
    //元素的剩余次数
    Map<Integer, Integer> elementRemainTimes;

    public Map<Integer, Integer> getElementRemainTimes() {
        return elementRemainTimes;
    }

    public void setElementRemainTimes(Map<Integer, Integer> elementRemainTimes) {
        this.elementRemainTimes = elementRemainTimes;
    }
}
