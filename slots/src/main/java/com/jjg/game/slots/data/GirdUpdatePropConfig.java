package com.jjg.game.slots.data;

/**
 * @author 11
 * @date 2025/7/9 16:28
 */
public class GirdUpdatePropConfig {
    private int id;
    //需要出现的元素权重
    private PropInfo showIconPropInfo;
    //影响格子权重
    private PropInfo affectGirdPropInfo;
    //随机次数权重
    private PropInfo randCountPropInfo;
    //每次成功后赋值权重
    private PropInfo valuePropInfo;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public PropInfo getShowIconPropInfo() {
        return showIconPropInfo;
    }

    public void setShowIconPropInfo(PropInfo showIconPropInfo) {
        this.showIconPropInfo = showIconPropInfo;
    }

    public PropInfo getAffectGirdPropInfo() {
        return affectGirdPropInfo;
    }

    public void setAffectGirdPropInfo(PropInfo affectGirdPropInfo) {
        this.affectGirdPropInfo = affectGirdPropInfo;
    }

    public PropInfo getRandCountPropInfo() {
        return randCountPropInfo;
    }

    public void setRandCountPropInfo(PropInfo randCountPropInfo) {
        this.randCountPropInfo = randCountPropInfo;
    }

    public PropInfo getValuePropInfo() {
        return valuePropInfo;
    }

    public void setValuePropInfo(PropInfo valuePropInfo) {
        this.valuePropInfo = valuePropInfo;
    }
}
