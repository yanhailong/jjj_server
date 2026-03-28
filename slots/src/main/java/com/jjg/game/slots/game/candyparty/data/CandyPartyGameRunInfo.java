package com.jjg.game.slots.game.candyparty.data;

import com.jjg.game.slots.data.GameRunInfo;

/**
 * @author 11
 * @date 2025/8/1 17:55
 */
public class CandyPartyGameRunInfo extends GameRunInfo<CandyPartyPlayerGameData> {
    //收集图标数量
    private int collectedIconNum;
    //当前层数
    private int layerNumber;
    //下轮层数
    private int nextLayerNumber;
    //剩余图标数量
    private int remainIconNum;
    //免费乘倍率
    private int freeGameMultiple;

    public CandyPartyGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public int getFreeGameMultiple() {
        return freeGameMultiple;
    }

    public void setFreeGameMultiple(int freeGameMultiple) {
        this.freeGameMultiple = freeGameMultiple;
    }

    public int getRemainIconNum() {
        return remainIconNum;
    }

    public void setRemainIconNum(int remainIconNum) {
        this.remainIconNum = remainIconNum;
    }

    public int getLayerNumber() {
        return layerNumber;
    }

    public void setLayerNumber(int layerNumber) {
        this.layerNumber = layerNumber;
    }

    public int getNextLayerNumber() {
        return nextLayerNumber;
    }

    public void setNextLayerNumber(int nextLayerNumber) {
        this.nextLayerNumber = nextLayerNumber;
    }

    public int getCollectedIconNum() {
        return collectedIconNum;
    }

    public void setCollectedIconNum(int collectedIconNum) {
        this.collectedIconNum = collectedIconNum;
    }
}
