package com.jjg.game.slots.game.wolfmoon.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@Document
public class WolfMoonPlayerGameData extends SlotsPlayerGameData {

    /**
     * 免费游戏类型
     * 1-高赔付符号 2-固定堆叠百搭符号 3-递增奖励倍数
     */
    private int freeGameType;

    /**
     * 当前递增奖励倍数（仅在递增奖励倍数模式下使用）
     */
    private int currentMultiplier = 5;

    public int getFreeGameType() {
        return freeGameType;
    }

    public void setFreeGameType(int freeGameType) {
        this.freeGameType = freeGameType;
    }

    public int getCurrentMultiplier() {
        return currentMultiplier;
    }

    public void setCurrentMultiplier(int currentMultiplier) {
        this.currentMultiplier = currentMultiplier;
    }

}
