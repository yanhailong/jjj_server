package com.jjg.game.slots.game.hulk.data;

/**
 * @author 11
 * @date 2026/4/29
 */
public class HulkInnerData {
    private int innerAuxiliaryIdex;
    //内层免费局的下标
    private int innerFreeGameIdex;
    private int innerStatus;
    private int innerRemainFreeCount;

    public int getInnerAuxiliaryIdex() {
        return innerAuxiliaryIdex;
    }

    public void setInnerAuxiliaryIdex(int innerAuxiliaryIdex) {
        this.innerAuxiliaryIdex = innerAuxiliaryIdex;
    }

    public int getInnerFreeGameIdex() {
        return innerFreeGameIdex;
    }

    public void setInnerFreeGameIdex(int innerFreeGameIdex) {
        this.innerFreeGameIdex = innerFreeGameIdex;
    }

    public int getInnerStatus() {
        return innerStatus;
    }

    public void setInnerStatus(int innerStatus) {
        this.innerStatus = innerStatus;
    }

    public int getInnerRemainFreeCount() {
        return innerRemainFreeCount;
    }

    public void setInnerRemainFreeCount(int innerRemainFreeCount) {
        this.innerRemainFreeCount = innerRemainFreeCount;
    }
}
