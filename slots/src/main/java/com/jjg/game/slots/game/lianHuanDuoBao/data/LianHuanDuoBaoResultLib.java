package com.jjg.game.slots.game.lianHuanDuoBao.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

/**
 * 连环夺宝结果库
 *
 * @author lm
 * @date 2026/6/2
 */
public class LianHuanDuoBaoResultLib extends SlotsResultLib<LianHuanDuoBaoAwardLineInfo> {
    //消除补齐的信息
    private List<LianHuanDuoBaoAddIconInfo> addIconInfos;
    //本局收集到的钥匙数量
    private int keyCollectionNum;
    //本局开启的宝箱奖励（含每个宝箱的奖金倍数 + 是否掉龙珠）
    private List<LianHuanDuoBaoChestReward> chestRewards;
    //本局通过连续消除（5 连）额外获得的龙珠数
    private int cascadeDragonBalls;
    //本局结算时玩家的关卡数（用于校验/客户端显示）
    private int layerNumber;

    public List<LianHuanDuoBaoAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<LianHuanDuoBaoAddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getKeyCollectionNum() {
        return keyCollectionNum;
    }

    public void setKeyCollectionNum(int keyCollectionNum) {
        this.keyCollectionNum = keyCollectionNum;
    }

    public List<LianHuanDuoBaoChestReward> getChestRewards() {
        return chestRewards;
    }

    public void setChestRewards(List<LianHuanDuoBaoChestReward> chestRewards) {
        this.chestRewards = chestRewards;
    }

    public int getCascadeDragonBalls() {
        return cascadeDragonBalls;
    }

    public void setCascadeDragonBalls(int cascadeDragonBalls) {
        this.cascadeDragonBalls = cascadeDragonBalls;
    }

    public int getLayerNumber() {
        return layerNumber;
    }

    public void setLayerNumber(int layerNumber) {
        this.layerNumber = layerNumber;
    }
}
