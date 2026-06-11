package com.jjg.game.sim.data;

import com.jjg.game.core.data.ItemOperationResult;

/**
 * @author 11
 * @date 2026/6/8
 */
public class SimItemOperationResult extends ItemOperationResult {
    //能量
    private int power;
    //知名度
    private int awareness;
    //研究点
    private int researchPoint;
    //稀有研究点
    private int rareResearchPoint;

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getAwareness() {
        return awareness;
    }

    public void setAwareness(int awareness) {
        this.awareness = awareness;
    }

    public int getResearchPoint() {
        return researchPoint;
    }

    public void setResearchPoint(int researchPoint) {
        this.researchPoint = researchPoint;
    }

    public int getRareResearchPoint() {
        return rareResearchPoint;
    }

    public void setRareResearchPoint(int rareResearchPoint) {
        this.rareResearchPoint = rareResearchPoint;
    }

    public static SimItemOperationResult createFromItemResult(ItemOperationResult itemResult) {
        SimItemOperationResult result = new SimItemOperationResult();
        result.setChangeBeforeItemNum(itemResult.getChangeBeforeItemNum());
        result.setChangeEndItemNum(itemResult.getChangeEndItemNum());
        result.setGoldNum(itemResult.getGoldNum());
        result.setChangeGoldNum(itemResult.getChangeGoldNum());
        result.setDiamond(itemResult.getDiamond());
        result.setChangeDiamondNum(itemResult.getChangeDiamondNum());
        result.setShell(itemResult.getShell());
        result.setChangeShellNum(itemResult.getChangeShellNum());
        return result;
    }
}
