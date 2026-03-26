package com.jjg.game.slots.game.mahjiongwin2.data;

/**
 * @author 11
 * @date 2025/9/10 9:42
 */
public class MahjiongWin2AddFreeInfo {
    //什么类型触发
    private int libType;
    //目标图标
    private int targetIcon;
    //增加次数
    private int addFreeCount;
    //概率
    private int prop;

    public int getLibType() {
        return libType;
    }

    public void setLibType(int libType) {
        this.libType = libType;
    }

    public int getTargetIcon() {
        return targetIcon;
    }

    public void setTargetIcon(int targetIcon) {
        this.targetIcon = targetIcon;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }

    public int getProp() {
        return prop;
    }

    public void setProp(int prop) {
        this.prop = prop;
    }
}
