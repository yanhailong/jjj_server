package com.jjg.game.slots.game.dracula.data;

/**
 * 百搭赐福配置（SpecialPlay 表 playType=3）
 * <p>
 * 格式：{@code modeId_pickCount}，例如 {@code 3_3} 表示 modeId=3 这个百搭赐福模式下，
 * 从盘面上所有带银框的元素里随机抽取 3 个，转换为 wild 符号。
 */
public class DraculaWildBlessingInfo {
    /** 触发模式 id（百搭赐福模式标识，对应 SpecialMode 表中的模式 id） */
    private int modeId;
    /** 随机抽取的银框符号个数 */
    private int pickCount;

    public int getModeId() {
        return modeId;
    }

    public void setModeId(int modeId) {
        this.modeId = modeId;
    }

    public int getPickCount() {
        return pickCount;
    }

    public void setPickCount(int pickCount) {
        this.pickCount = pickCount;
    }
}
