package com.jjg.game.slots.game.lianHuanDuoBao.data;

import com.jjg.game.slots.data.GameRunInfo;

import java.util.List;

/**
 * 连环夺宝单局运行信息
 *
 * @author lm
 * @date 2026/6/2
 */
public class LianHuanDuoBaoGameRunInfo extends GameRunInfo<LianHuanDuoBaoPlayerGameData> {
    //本局收集的钥匙数
    private int collectedKeyNum;
    //本局结束后玩家累计钥匙数
    private int totalKeyNum;
    //本局结算后玩家累计龙珠数
    private int dragonBallCount;
    //本局所在关卡
    private int layerNumber;
    //下局所在关卡（如果本局凑齐 15 钥匙就会切换）
    private int nextLayerNumber;
    //本局聚宝盆增减量（正数=进入聚宝盆，负数=从聚宝盆释放给玩家）
    private long treasureBowlDelta;
    //本局结算后聚宝盆余额
    private long treasureBowlAmount;
    //本局开启的宝箱奖励列表
    private List<LianHuanDuoBaoChestReward> chestRewards;
    //bonus 小游戏：本次抽中的奖金倍数列表（按龙珠掉落顺序）
    private List<Long> bonusBallWins;
    //bonus 小游戏总赢金
    private long bonusTotalWin;

    public LianHuanDuoBaoGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public int getCollectedKeyNum() {
        return collectedKeyNum;
    }

    public void setCollectedKeyNum(int collectedKeyNum) {
        this.collectedKeyNum = collectedKeyNum;
    }

    public int getTotalKeyNum() {
        return totalKeyNum;
    }

    public void setTotalKeyNum(int totalKeyNum) {
        this.totalKeyNum = totalKeyNum;
    }

    public int getDragonBallCount() {
        return dragonBallCount;
    }

    public void setDragonBallCount(int dragonBallCount) {
        this.dragonBallCount = dragonBallCount;
    }

    public int getLayerNumber() {
        return layerNumber;
    }

    public void setLayerNumber(int layerNumber) {
        this.layerNumber = layerNumber;
    }

    public int getNextLayerNumber() {
        return nextLayerNumber;
    }

    public void setNextLayerNumber(int nextLayerNumber) {
        this.nextLayerNumber = nextLayerNumber;
    }

    public long getTreasureBowlDelta() {
        return treasureBowlDelta;
    }

    public void setTreasureBowlDelta(long treasureBowlDelta) {
        this.treasureBowlDelta = treasureBowlDelta;
    }

    public long getTreasureBowlAmount() {
        return treasureBowlAmount;
    }

    public void setTreasureBowlAmount(long treasureBowlAmount) {
        this.treasureBowlAmount = treasureBowlAmount;
    }

    public List<LianHuanDuoBaoChestReward> getChestRewards() {
        return chestRewards;
    }

    public void setChestRewards(List<LianHuanDuoBaoChestReward> chestRewards) {
        this.chestRewards = chestRewards;
    }

    public List<Long> getBonusBallWins() {
        return bonusBallWins;
    }

    public void setBonusBallWins(List<Long> bonusBallWins) {
        this.bonusBallWins = bonusBallWins;
    }

    public long getBonusTotalWin() {
        return bonusTotalWin;
    }

    public void setBonusTotalWin(long bonusTotalWin) {
        this.bonusTotalWin = bonusTotalWin;
    }
}
