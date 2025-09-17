package com.jjg.game.core.data;

import java.util.Map;

/**
 * 道具操作结果
 *
 * @author lm
 * @date 2025/9/16 14:40
 */
public class ItemOperationResult {
    //变化前的道具数量
    private Map<Integer, Long> changeBeforeItemNum;
    //变化后的道具数量
    private Map<Integer, Long> changeEndItemNum;
    //变化后的金币数量
    private long goldNum;
    //变化后的钻石数量
    private long diamond;

    public Map<Integer, Long> getChangeEndItemNum() {
        return changeEndItemNum;
    }

    public void setChangeEndItemNum(Map<Integer, Long> changeEndItemNum) {
        this.changeEndItemNum = changeEndItemNum;
    }

    public Map<Integer, Long> getChangeBeforeItemNum() {
        return changeBeforeItemNum;
    }

    public void setChangeBeforeItemNum(Map<Integer, Long> changeBeforeItemNum) {
        this.changeBeforeItemNum = changeBeforeItemNum;
    }

    public long getGoldNum() {
        return goldNum;
    }

    public void setGoldNum(long goldNum) {
        this.goldNum = goldNum;
    }

    public long getDiamond() {
        return diamond;
    }

    public void setDiamond(long diamond) {
        this.diamond = diamond;
    }
}
