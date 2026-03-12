package com.jjg.game.activity.continuousRecharge.data;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * @author 11
 * @date 2026/3/5
 */
public class DailyContinuousData {
    //第几天
    private int index;
    //日期
    private int date;
    //当日充值
    private BigDecimal rechargeNum;
    //已达成的任务配置ID集合（防止重复累加返利比例）
    private Set<Integer> claimedTaskIds;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

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

    public Set<Integer> getClaimedTaskIds() {
        return claimedTaskIds;
    }

    public void setClaimedTaskIds(Set<Integer> claimedTaskIds) {
        this.claimedTaskIds = claimedTaskIds;
    }

    /**
     * 更新充值金额
     *
     * @param addRechargeNum
     */
    public void addRechargeNum(BigDecimal addRechargeNum) {
        if (this.rechargeNum == null) {
            this.rechargeNum = addRechargeNum;
        } else {
            this.rechargeNum = this.rechargeNum.add(addRechargeNum);
        }
    }

    /**
     * 是否完成某个配置id
     *
     * @param taskId
     * @return
     */
    public boolean containsClaimedTaskId(int taskId) {
        if (this.claimedTaskIds == null) {
            return false;
        }
        return this.claimedTaskIds.contains(taskId);
    }

    /**
     * 标记指定任务已达成
     *
     * @param taskId
     */
    public void markTaskClaimed(int taskId) {
        if (this.claimedTaskIds == null) {
            this.claimedTaskIds = new HashSet<>();
        }
        this.claimedTaskIds.add(taskId);
    }

    /**
     * 检查是否可以进行下一天
     * @return
     */
    public boolean canNext(){
        if(this.claimedTaskIds != null && !this.claimedTaskIds.isEmpty()){
            return true;
        }
        return false;
    }
}
