package com.jjg.game.hall.minigame.game.luckytreasure.message.bean;


import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.minigame.game.luckytreasure.bean.LuckyTreasureConsumeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 夺宝奇兵详情
 */
@ProtobufMessage
@ProtoDesc("夺宝奇兵详情")
public class LuckyTreasureInfo {

    /**
     * 期号
     */
    @ProtoDesc("期号")
    private long issueNumber;

    /**
     * 配置id
     */
    @ProtoDesc("配置id")
    private int configId;

    /**
     * 商品类型
     */
    @ProtoDesc("商品类型")
    private int type;

    /**
     * 商品道具id
     */
    @ProtoDesc("商品道具id")
    private int itemId;

    /**
     * 商品道具数量
     */
    @ProtoDesc("商品道具数量")
    private int itemNum;

    /**
     * 商品价值
     */
    @ProtoDesc("商品价值")
    private int bestValue;

    /**
     * 总数量
     */
    @ProtoDesc("总数量")
    private int totalCount;

    /**
     * 已售出数量
     */
    @ProtoDesc("已售出数量")
    private int soldCount;

    /**
     * 购买单个所需的消耗
     */
    @ProtoDesc("购买单个所需的消耗")
    private List<LuckyTreasureConsumeInfo> consumeInfoList = new ArrayList<>();

    /**
     * 商品icon
     */
    @ProtoDesc("商品icon")
    private String icon;

    /**
     * 商品名字
     */
    @ProtoDesc("商品名字")
    private String name;

    /**
     * 参与人数
     */
    @ProtoDesc("参与人数")
    private int buyCount;

    /**
     * 已经购买的数量
     */
    @ProtoDesc("已经购买的数量")
    private int alreadyBuyCount;

    /**
     * 当前状态 1=可购买,2=等待开奖,3=待领取,4=已领取,5=领奖结束(中奖未领取),6=未中奖
     */
    @ProtoDesc("当前状态 1=可购买,2=等待开奖,3=待领取,4=已领取,5=领奖结束(中奖未领取),6=未中奖")
    private int status;

    /**
     * 开奖倒计时
     */
    @ProtoDesc("开奖倒计时")
    private int countDown;

    /**
     * 领奖倒计时
     */
    @ProtoDesc("领奖倒计时")
    private int receiveCountdown;

    /**
     * 第三方领奖码
     */
    @ProtoDesc("第三方领奖码")
    private String rewardCode;

    @ProtoDesc("结束购买倒计时")
    private int endBuyCountDown;

    public long getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(long issueNumber) {
        this.issueNumber = issueNumber;
    }

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getItemNum() {
        return itemNum;
    }

    public void setItemNum(int itemNum) {
        this.itemNum = itemNum;
    }

    public int getBestValue() {
        return bestValue;
    }

    public void setBestValue(int bestValue) {
        this.bestValue = bestValue;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(int soldCount) {
        this.soldCount = soldCount;
    }

    public List<LuckyTreasureConsumeInfo> getConsumeInfoList() {
        return consumeInfoList;
    }

    public void setConsumeInfoList(List<LuckyTreasureConsumeInfo> consumeInfoList) {
        this.consumeInfoList = consumeInfoList;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBuyCount() {
        return buyCount;
    }

    public void setBuyCount(int buyCount) {
        this.buyCount = buyCount;
    }

    public int getAlreadyBuyCount() {
        return alreadyBuyCount;
    }

    public void setAlreadyBuyCount(int alreadyBuyCount) {
        this.alreadyBuyCount = alreadyBuyCount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getCountDown() {
        return countDown;
    }

    public void setCountDown(int countDown) {
        this.countDown = countDown;
    }

    public int getReceiveCountdown() {
        return receiveCountdown;
    }

    public void setReceiveCountdown(int receiveCountdown) {
        this.receiveCountdown = receiveCountdown;
    }

    public String getRewardCode() {
        return rewardCode;
    }

    public void setRewardCode(String rewardCode) {
        this.rewardCode = rewardCode;
    }

    public int getEndBuyCountDown() {
        return endBuyCountDown;
    }

    public void setEndBuyCountDown(int endBuyCountDown) {
        this.endBuyCountDown = endBuyCountDown;
    }
}
