package com.jjg.game.slots.data;

import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/7/9 16:28
 */
public class GirdUpdateConfig {
    private int id;
    //特殊元素id
    private int specialIconId;
    //特殊元素权重
    private PropInfo specialIconPropInfo;
    //其他元素权重
    private PropInfo otherIconPropInfo;
    //特殊元素影响格子权重
    private PropInfo specialIconAffectGirdPropInfo;
    //其他元素影响格子权重
    private PropInfo otherIconAffectGirdPropInfo;
    //随机次数权重
    private PropInfo randCountPropInfo;
    //巨型块
    private PropInfo bigBlockSizePropInfo;
    private PropInfo specialBigBlockSizePropInfo;
    //每个图标对应的列
    private Map<Integer, List<Integer>> iconColumMap;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSpecialIconId() {
        return specialIconId;
    }

    public void setSpecialIconId(int specialIconId) {
        this.specialIconId = specialIconId;
    }

    public PropInfo getSpecialIconPropInfo() {
        return specialIconPropInfo;
    }

    public void setSpecialIconPropInfo(PropInfo specialIconPropInfo) {
        this.specialIconPropInfo = specialIconPropInfo;
    }

    public PropInfo getOtherIconPropInfo() {
        return otherIconPropInfo;
    }

    public void setOtherIconPropInfo(PropInfo otherIconPropInfo) {
        this.otherIconPropInfo = otherIconPropInfo;
    }

    public PropInfo getSpecialIconAffectGirdPropInfo() {
        return specialIconAffectGirdPropInfo;
    }

    public void setSpecialIconAffectGirdPropInfo(PropInfo specialIconAffectGirdPropInfo) {
        this.specialIconAffectGirdPropInfo = specialIconAffectGirdPropInfo;
    }

    public PropInfo getOtherIconAffectGirdPropInfo() {
        return otherIconAffectGirdPropInfo;
    }

    public void setOtherIconAffectGirdPropInfo(PropInfo otherIconAffectGirdPropInfo) {
        this.otherIconAffectGirdPropInfo = otherIconAffectGirdPropInfo;
    }

    public PropInfo getRandCountPropInfo() {
        return randCountPropInfo;
    }

    public void setRandCountPropInfo(PropInfo randCountPropInfo) {
        this.randCountPropInfo = randCountPropInfo;
    }

    public PropInfo getBigBlockSizePropInfo() {
        return bigBlockSizePropInfo;
    }

    public void setBigBlockSizePropInfo(PropInfo bigBlockSizePropInfo) {
        this.bigBlockSizePropInfo = bigBlockSizePropInfo;
    }

    public PropInfo getSpecialBigBlockSizePropInfo() {
        return specialBigBlockSizePropInfo;
    }

    public void setSpecialBigBlockSizePropInfo(PropInfo specialBigBlockSizePropInfo) {
        this.specialBigBlockSizePropInfo = specialBigBlockSizePropInfo;
    }

    public Map<Integer, List<Integer>> getIconColumMap() {
        return iconColumMap;
    }

    public void setIconColumMap(Map<Integer, List<Integer>> iconColumMap) {
        this.iconColumMap = iconColumMap;
    }
}
