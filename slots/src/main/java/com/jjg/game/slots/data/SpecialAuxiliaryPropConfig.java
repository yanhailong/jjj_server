package com.jjg.game.slots.data;

/**
 * @author 11
 * @date 2025/8/30 11:09
 */
public class SpecialAuxiliaryPropConfig {
    private int id;
    //免费旋转次数权重
    private PropInfo triggerCountPropInfo;
    //随机次数权重
    private PropInfo randCountPropInfo;
    //修改图案策略组
    private PropInfo specialGroupGirdIDPropInfo;
    //奖励c
    private PropInfo awardTypeCPropInfo;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public PropInfo getTriggerCountPropInfo() {
        return triggerCountPropInfo;
    }

    public void setTriggerCountPropInfo(PropInfo triggerCountPropInfo) {
        this.triggerCountPropInfo = triggerCountPropInfo;
    }

    public PropInfo getRandCountPropInfo() {
        return randCountPropInfo;
    }

    public void setRandCountPropInfo(PropInfo randCountPropInfo) {
        this.randCountPropInfo = randCountPropInfo;
    }

    public PropInfo getSpecialGroupGirdIDPropInfo() {
        return specialGroupGirdIDPropInfo;
    }

    public void setSpecialGroupGirdIDPropInfo(PropInfo specialGroupGirdIDPropInfo) {
        this.specialGroupGirdIDPropInfo = specialGroupGirdIDPropInfo;
    }

    public PropInfo getAwardTypeCPropInfo() {
        return awardTypeCPropInfo;
    }

    public void setAwardTypeCPropInfo(PropInfo awardTypeCPropInfo) {
        this.awardTypeCPropInfo = awardTypeCPropInfo;
    }
}
