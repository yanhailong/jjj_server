package com.jjg.game.activity.continuousRecharge.data;

import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.constant.ActivityConstant;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 连续充值活动数据
 *
 * @author 11
 * @date 2026/3/5
 */
public class ContinuousRechargeActivityData extends PlayerActivityData {
    //七日连充中每天的充值数据 dailyIndex -> DailyRechargeData
    private Map<Integer, DailyContinuousData> dailyRechargeMap;
    //福利数据(每日充值)
    private DailyWelfareData dailyWelfareData;
    //福利活动月奖励已领取  cfgId
    private Set<Integer> welfarReceSet;
    //七日连充期间的累计充值总额（不受每日清除影响）
    private BigDecimal continuousTotalRecharge;
    //福利活动当月累计充值数量
    private BigDecimal welfarMonthRechargeNum;
    //参与时间
    private long joinTime;
    //当前连充天数索引（0-6）
    private int currentDayIndex;
    //累计返利比例（万分比）
    private int totalRebateRate;
    //最后检查日期（yyyyMMdd 格式）
    private int lastCheckDate;
    //七日连充实际返利金额(已领取的)
    private long rebateGoldNum;

    public Map<Integer, DailyContinuousData> getDailyRechargeMap() {
        return dailyRechargeMap;
    }

    public void setDailyRechargeMap(Map<Integer, DailyContinuousData> dailyRechargeMap) {
        this.dailyRechargeMap = dailyRechargeMap;
    }

    public DailyWelfareData getDailyWelfareData() {
        return dailyWelfareData;
    }

    public void setDailyWelfareData(DailyWelfareData dailyWelfareData) {
        this.dailyWelfareData = dailyWelfareData;
    }

    public Set<Integer> getWelfarReceSet() {
        return welfarReceSet;
    }

    public void setWelfarReceSet(Set<Integer> welfarReceSet) {
        this.welfarReceSet = welfarReceSet;
    }

    public BigDecimal getContinuousTotalRecharge() {
        return continuousTotalRecharge;
    }

    public void setContinuousTotalRecharge(BigDecimal continuousTotalRecharge) {
        this.continuousTotalRecharge = continuousTotalRecharge;
    }

    public BigDecimal getWelfarMonthRechargeNum() {
        return welfarMonthRechargeNum;
    }

    public void setWelfarMonthRechargeNum(BigDecimal welfarMonthRechargeNum) {
        this.welfarMonthRechargeNum = welfarMonthRechargeNum;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }

    public int getCurrentDayIndex() {
        return currentDayIndex;
    }

    public void setCurrentDayIndex(int currentDayIndex) {
        this.currentDayIndex = currentDayIndex;
    }

    public int getTotalRebateRate() {
        return totalRebateRate;
    }

    public void setTotalRebateRate(int totalRebateRate) {
        this.totalRebateRate = totalRebateRate;
    }

    public int getLastCheckDate() {
        return lastCheckDate;
    }

    public void setLastCheckDate(int lastCheckDate) {
        this.lastCheckDate = lastCheckDate;
    }

    public long getRebateGoldNum() {
        return rebateGoldNum;
    }

    public void setRebateGoldNum(long rebateGoldNum) {
        this.rebateGoldNum = rebateGoldNum;
    }

    public DailyContinuousData queryDailyContinuousByDay(int daiIndex) {
        if (this.dailyRechargeMap == null || this.dailyRechargeMap.isEmpty()) {
            return null;
        }
        return this.dailyRechargeMap.get(daiIndex);
    }

    /**
     * 更新连续充值中当天充值记录
     *
     * @param dayIndex    当天索引（0-6）
     * @param rechargeNum 充值金额
     * @param date  日期
     * @return
     */
    public DailyContinuousData updateDailyContinuousData(int dayIndex, BigDecimal rechargeNum, int date) {
        if (this.dailyRechargeMap == null) {
            this.dailyRechargeMap = new HashMap<>();
        }

        DailyContinuousData data = this.dailyRechargeMap.get(dayIndex);
        if (data == null) {
            data = new DailyContinuousData();
            data.setIndex(dayIndex);
            data.setDate(date);
            data.setRechargeNum(rechargeNum);
        } else {
            //更新当日充值额
            data.addRechargeNum(rechargeNum);
        }

        if(this.continuousTotalRecharge == null){
            this.continuousTotalRecharge = rechargeNum;
        }else {
            this.continuousTotalRecharge = this.continuousTotalRecharge.add(rechargeNum);
        }
        this.dailyRechargeMap.put(dayIndex, data);
        return data;
    }

    /**
     * 更新福利活动充值数量
     *
     * @param rechargeNum
     */
    public void updateWelfareRechargeData(int date, BigDecimal rechargeNum) {
        //判断是不是跨天
        if (this.dailyWelfareData == null || this.dailyWelfareData.getDate() != date) {
            this.dailyWelfareData = new DailyWelfareData();
            this.dailyWelfareData.setDate(date);
        }

        this.dailyWelfareData.addRechargeNum(rechargeNum);

        if (this.welfarMonthRechargeNum == null) {
            this.welfarMonthRechargeNum = rechargeNum;
        } else {
            this.welfarMonthRechargeNum = this.welfarMonthRechargeNum.add(rechargeNum);
        }
    }

    /**
     * 检查最新的一天是否完成
     *
     * @return
     */
    public boolean currentDayCompelete() {
        DailyContinuousData lastDayData = queryDailyContinuousByDay(this.currentDayIndex);
        return lastDayData != null && lastDayData.canNext();
    }

    /**
     * 计算七日连充累计充值总额
     *
     * @return
     */
    public BigDecimal calculateContinuousTotalRecharge() {
        if (this.dailyRechargeMap == null || this.dailyRechargeMap.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (DailyContinuousData d : this.dailyRechargeMap.values()) {
            if(d.getRechargeNum() == null){
                continue;
            }
            total = total.add(d.getRechargeNum());
        }
        return total;
    }

    /**
     * 是否领取福利活动的奖励
     *
     * @param cfgId
     * @return
     */
    public boolean welfarRece(int cfgId) {
        if (this.welfarReceSet == null || this.welfarReceSet.isEmpty()) {
            return false;
        }
        return this.welfarReceSet.contains(cfgId);
    }

    public void welfareReward(int cfgId){
        if (this.welfarReceSet == null) {
            this.welfarReceSet = new HashSet<>();
        }
        this.welfarReceSet.add(cfgId);
    }

    /**
     * 清除最近一天的充值数据
     */
    public void clearContinuousCurrentData(){
        DailyContinuousData data = queryDailyContinuousByDay(this.currentDayIndex);
        if(data == null){
            return;
        }
        data.setRechargeNum(null);
        data.setClaimedTaskIds(null);
    }
}
