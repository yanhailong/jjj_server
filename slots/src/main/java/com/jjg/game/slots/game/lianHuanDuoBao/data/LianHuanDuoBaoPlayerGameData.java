package com.jjg.game.slots.game.lianHuanDuoBao.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 连环夺宝玩家数据
 *
 * @author lm
 * @date 2026/6/2
 */
@Document
public class LianHuanDuoBaoPlayerGameData extends SlotsPlayerGameData {
    //当前关卡数（1/2/3，分别对应 4x4 / 5x5 / 6x6）
    private int layerNumber = 1;
    //已收集的钥匙数（达到 15 进入下一关）
    private int collectedKeyNum;
    //已收集的龙珠数（用于 bonus 小游戏）
    private int dragonBallCount;
    //聚宝盆金额（玩家个人小池，bonus 通关后清零）
    private long treasureBowlAmount;
    //当前关卡已开启的宝箱数量（0..15），关卡切换时重置为 0
    private int openedChestCount;

    public int getLayerNumber() {
        return layerNumber;
    }

    public void setLayerNumber(int layerNumber) {
        this.layerNumber = layerNumber;
    }

    public int getCollectedKeyNum() {
        return collectedKeyNum;
    }

    public void setCollectedKeyNum(int collectedKeyNum) {
        this.collectedKeyNum = collectedKeyNum;
    }

    public int getDragonBallCount() {
        return dragonBallCount;
    }

    public void setDragonBallCount(int dragonBallCount) {
        this.dragonBallCount = dragonBallCount;
    }

    public long getTreasureBowlAmount() {
        return treasureBowlAmount;
    }

    public void setTreasureBowlAmount(long treasureBowlAmount) {
        this.treasureBowlAmount = treasureBowlAmount;
    }

    public int getOpenedChestCount() {
        return openedChestCount;
    }

    public void setOpenedChestCount(int openedChestCount) {
        this.openedChestCount = openedChestCount;
    }
}
