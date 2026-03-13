package com.jjg.game.activity.continuousRecharge.data;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * 福利数据
 *
 * @author 11
 * @date 2026/3/5
 */
public class DailyWelfareData {
    //年月日 yyMMdd格式
    private int date;
    //充值金额
    private BigDecimal rechargeNum;
    //已领取奖励  cfgId
    private Set<Integer> receSet;

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public BigDecimal getRechargeNum() {
        return rechargeNum;
    }

    public void setRechargeNum(BigDecimal rechargeNum) {
        this.rechargeNum = rechargeNum;
    }

    public Set<Integer> getReceSet() {
        return receSet;
    }

    public void setReceSet(Set<Integer> receSet) {
        this.receSet = receSet;
    }

    public void addRechargeNum(BigDecimal rechargeNum) {
        if (this.rechargeNum == null) {
            this.rechargeNum = rechargeNum;
        } else {
            this.rechargeNum = this.rechargeNum.add(rechargeNum);
        }
    }

    public boolean rece(int cfgId) {
        if (this.receSet == null || this.receSet.isEmpty()) {
            return false;
        }
        return this.receSet.contains(cfgId);
    }

    public void receReward(int cfgId) {
        if (this.receSet == null) {
            this.receSet = new HashSet<>();
        }
        this.receSet.add(cfgId);
    }
}
