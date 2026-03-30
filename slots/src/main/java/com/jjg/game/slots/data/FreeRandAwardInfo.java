package com.jjg.game.slots.data;

/**
 * @author 11
 * @date 2025/7/7 11:13
 */
public class FreeRandAwardInfo {
    //模式id
    private int modelId;
    //小游戏奖励id
    private int awardId;
    //权重
    private int prop;
    //最大次数限制
    private int maxLimit;

    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    public int getAwardId() {
        return awardId;
    }

    public void setAwardId(int awardId) {
        this.awardId = awardId;
    }

    public int getProp() {
        return prop;
    }

    public void setProp(int prop) {
        this.prop = prop;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }
}
