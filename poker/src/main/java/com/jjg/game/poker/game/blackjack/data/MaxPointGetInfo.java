package com.jjg.game.poker.game.blackjack.data;

/**
 * @author lm
 * @date 2025/8/8 13:45
 */
public class MaxPointGetInfo {
    //最大点数
    private int maxPoint;
    //取牌截止索引
    private int index;
    //是否是软手
    private final boolean isSoftHand;

    public MaxPointGetInfo(int maxPoint, int index, boolean isSoftHand) {
        this.maxPoint = maxPoint;
        this.index = index;
        this.isSoftHand = isSoftHand;
    }

    public boolean isSoftHand() {
        return isSoftHand;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getMaxPoint() {
        return maxPoint;
    }

    public void setMaxPoint(int maxPoint) {
        this.maxPoint = maxPoint;
    }
}
