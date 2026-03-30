package com.jjg.game.slots.game.superstar.data;

import com.jjg.game.slots.data.AwardLineInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 超级明星奖励线信息
 */
public class SuperStarAwardLineInfo extends AwardLineInfo {

    /**
     * 图标数量
     */
    private int sameCount;

    /**
     * 线id
     */
    private int lineId;

    /**
     * 倍数
     */
    private int baseTimes;

    /**
     * 触发的jackpotId
     */
    private int jackpotId;

    /**
     * 其他图标加成倍数
     */
    protected Map<Integer, Integer> otherIconAwardInfoMap;


    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }

    public int getSameCount() {
        return sameCount;
    }

    public void setSameCount(int sameCount) {
        this.sameCount = sameCount;
    }

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public Map<Integer, Integer> getOtherIconAwardInfoMap() {
        return otherIconAwardInfoMap;
    }

    public void setOtherIconAwardInfoMap(Map<Integer, Integer> otherIconAwardInfoMap) {
        this.otherIconAwardInfoMap = otherIconAwardInfoMap;
    }

    public void addSpecialAwardInfo(int iconId, int times) {
        if (this.otherIconAwardInfoMap == null) {
            this.otherIconAwardInfoMap = new HashMap<>();
        }

        this.otherIconAwardInfoMap.put(iconId, times);
    }

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }
}
