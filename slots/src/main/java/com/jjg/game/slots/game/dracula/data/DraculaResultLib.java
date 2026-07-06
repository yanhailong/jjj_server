package com.jjg.game.slots.game.dracula.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:30
 */
public class DraculaResultLib extends SlotsResultLib<DraculaAwardLineInfo> {
    //消除补齐的信息
    private List<DraculaAddIconInfo> addIconInfos;
    //本局触发再次免费的局数（仅 FREE 模式内用：盘面 4+ scatter 时按文档表加局数）
    private int addFreeCount;
    //本局应用的最终乘倍值
    //  主游戏：消除轮数 1/2/3/4/≥5 → x1/x2/x3/x4/x5
    //  免费游戏：消除轮数 1/2/3/4/≥5 → x3/x6/x9/x12/x15
    //多轮 cascade 时取最后一轮（即最高一轮）；无中奖时为 1
    private int multiplier = 1;

    public List<DraculaAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<DraculaAddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }
}
