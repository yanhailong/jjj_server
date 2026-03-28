package com.jjg.game.slots.game.candyparty.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
public class CandyPartyResultLib extends SlotsResultLib<CandyPartyAwardLineInfo> {
    //消除补齐的信息
    private List<CandyPartyAddIconInfo> addIconInfos;
    //收集的元素数量
    private int elementCollectionNum;
    //免费游戏乘倍数
    private int freeGameMultiple;

    public int getFreeGameMultiple() {
        return freeGameMultiple;
    }

    public void setFreeGameMultiple(int freeGameMultiple) {
        this.freeGameMultiple = freeGameMultiple;
    }

    public List<CandyPartyAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<CandyPartyAddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getElementCollectionNum() {
        return elementCollectionNum;
    }

    public void setElementCollectionNum(int elementCollectionNum) {
        this.elementCollectionNum = elementCollectionNum;
    }

}
