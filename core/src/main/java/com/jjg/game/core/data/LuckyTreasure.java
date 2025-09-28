package com.jjg.game.core.data;

import com.jjg.game.core.config.bean.LuckyTreasureConfig;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 夺宝奇兵数据
 */
@Document
//@CompoundIndex(name = "endTime_idx", def = "{'endTime': -1}")
//@CompoundIndex(name = "startTime_idx", def = "{'startTime': -1}")
//@CompoundIndex(name = "issueNumber_idx", def = "{'issueNumber': -1}")
//@CompoundIndex(name = "rewardCode_idx", def = "{'rewardCode': 1}", unique = true)
public class LuckyTreasure {

    /**
     * 期号
     */
    @Id
    private long issueNumber;

    /**
     * 本次所使用的配置
     */
    private LuckyTreasureConfig config;

    /**
     * 购买数据map k=玩家id v=购买数量
     */
    private Map<Long, Integer> buyMap = new HashMap<>();

    /**
     * 已经售出的数量
     */
    private int soldCount;

    /**
     * 开始时间
     */
    private long startTime;

    /**
     * 结束时间
     */
    private long endTime;

    /**
     * 获奖者id
     */
    private long awardPlayerId;


    /**
     * 获奖者名字
     */
    private String awardPlayerNickName;

    /**
     * 获奖者头像
     */
    private int awardPlayerHeadImgId;

    /**
     * 头像框id
     */
    private int awardPlayerHeadFrameId;

    /**
     * 国旗id
     */
    private int awardPlayerNationalId;

    /**
     * 第三方领奖码
     */
    private String rewardCode;

    /**
     * 玩家是否已领奖（仅type=1时需要）
     */
    private boolean received;

    /**
     * 领奖时间
     */
    private long receiveTime;

    /**
     * 购买记录
     */
    private List<LuckyTreasureBuyRecord> buyRecordList = new ArrayList<>();

    public Long getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(long issueNumber) {
        this.issueNumber = issueNumber;
    }

    public Map<Long, Integer> getBuyMap() {
        return buyMap;
    }

    public void setBuyMap(Map<Long, Integer> buyMap) {
        this.buyMap = buyMap;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getAwardPlayerId() {
        return awardPlayerId;
    }

    public void setAwardPlayerId(long awardPlayerId) {
        this.awardPlayerId = awardPlayerId;
    }

    public LuckyTreasureConfig getConfig() {
        return config;
    }

    public void setConfig(LuckyTreasureConfig config) {
        this.config = config;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(int soldCount) {
        this.soldCount = soldCount;
    }

    public String getRewardCode() {
        return rewardCode;
    }

    public void setRewardCode(String rewardCode) {
        this.rewardCode = rewardCode;
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }

    public long getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(long receiveTime) {
        this.receiveTime = receiveTime;
    }

    public List<LuckyTreasureBuyRecord> getBuyRecordList() {
        return buyRecordList;
    }

    public void setBuyRecordList(List<LuckyTreasureBuyRecord> buyRecordList) {
        this.buyRecordList = buyRecordList;
    }

    public String getAwardPlayerNickName() {
        return awardPlayerNickName;
    }

    public void setAwardPlayerNickName(String awardPlayerNickName) {
        this.awardPlayerNickName = awardPlayerNickName;
    }

    public int getAwardPlayerHeadImgId() {
        return awardPlayerHeadImgId;
    }

    public void setAwardPlayerHeadImgId(int awardPlayerHeadImgId) {
        this.awardPlayerHeadImgId = awardPlayerHeadImgId;
    }

    public int getAwardPlayerHeadFrameId() {
        return awardPlayerHeadFrameId;
    }

    public void setAwardPlayerHeadFrameId(int awardPlayerHeadFrameId) {
        this.awardPlayerHeadFrameId = awardPlayerHeadFrameId;
    }

    public int getAwardPlayerNationalId() {
        return awardPlayerNationalId;
    }

    public void setAwardPlayerNationalId(int awardPlayerNationalId) {
        this.awardPlayerNationalId = awardPlayerNationalId;
    }
}
