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
    //金币变化值
    private long changeGoldNum;
    //变化后的钻石数量
    private long diamond;
    //钻石变化值
    private long changeDiamondNum;

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

    public long getChangeGoldNum() {
        return changeGoldNum;
    }

    public void setChangeGoldNum(long changeGoldNum) {
        this.changeGoldNum = changeGoldNum;
    }

    public long getChangeDiamondNum() {
        return changeDiamondNum;
    }

    public void setChangeDiamondNum(long changeDiamondNum) {
        this.changeDiamondNum = changeDiamondNum;
    }

    public void goldChange(long changeNum,long afterNum) {
        this.goldNum = afterNum;
        this.changeGoldNum = changeNum;
    }

    public void diamondChange(long changeNum,long afterNum) {
        this.diamond = afterNum;
        this.changeDiamondNum = changeNum;
    }
}
