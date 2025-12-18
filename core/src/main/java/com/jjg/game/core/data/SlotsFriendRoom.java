package com.jjg.game.core.data;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Document(collection = "SlotsFriendRoom")
public class SlotsFriendRoom extends FriendRoom {
    @Transient
    private transient AtomicLong totalFlowingA = new AtomicLong(0);
    @Transient
    private transient AtomicLong totalIncomeA = new AtomicLong(0);
    // 参与玩家收益 玩家ID + 收益数量
    @Transient
    private transient Map<Long, Long> partInPlayerIncomeA = new ConcurrentHashMap<>();
    // 玩家押分 玩家ID + 玩家押分
    @Transient
    private transient Map<Long, Long> partInPlayerBetA = new ConcurrentHashMap<>();



    @Transient
    private transient AtomicLong totalFlowingB = new AtomicLong(0);
    @Transient
    private transient AtomicLong totalIncomeB = new AtomicLong(0);
    // 参与玩家收益 玩家ID + 收益数量
    @Transient
    private transient Map<Long, Long> partInPlayerIncomeB = new ConcurrentHashMap<>();
    // 玩家押分 玩家ID + 玩家押分
    @Transient
    private transient Map<Long, Long> partInPlayerBetB = new ConcurrentHashMap<>();


    //为false时，使用A
    //为true时，使用B
    @Transient
    private transient final AtomicBoolean flag = new AtomicBoolean(false);


    /**
     * 玩家下注后修改信息
     * @param playerId
     * @param betValue
     * @param income
     */
    public void addBet(long playerId, long betValue, long income) {
        if(this.flag.get()){
            this.totalIncomeB.addAndGet(income);
            this.partInPlayerIncomeB.merge(playerId, income, Long::sum);

            this.totalFlowingB.addAndGet(betValue);
            this.partInPlayerBetB.merge(playerId, betValue, Long::sum);
        }else {
            this.totalIncomeA.addAndGet(income);
            this.partInPlayerIncomeA.merge(playerId, income, Long::sum);

            this.totalFlowingA.addAndGet(betValue);
            this.partInPlayerBetA.merge(playerId, betValue, Long::sum);
        }
    }

    /**
     * 切换 flag 的值
     * 上层已加锁
     * @return
     */
    public SlotsBillInfo toggleFlag(){
        boolean newFlag = !this.flag.get();
        this.flag.set(newFlag);

        SlotsBillInfo slotsBillInfo = new SlotsBillInfo();
        if(newFlag){
            slotsBillInfo.setTotalFlowing(this.totalFlowingA.getAndSet(0));
            slotsBillInfo.setTotalIncome(this.totalIncomeA.getAndSet(0));

            if(!this.partInPlayerBetA.isEmpty()){
                slotsBillInfo.setPartInPlayerBet(new HashMap<>(this.partInPlayerBetA));
            }
            this.partInPlayerBetA.clear();

            if(!this.partInPlayerIncomeA.isEmpty()){
                slotsBillInfo.setPartInPlayerIncome(new HashMap<>(this.partInPlayerIncomeA));
            }
            this.partInPlayerIncomeA.clear();
        }else {
            slotsBillInfo.setTotalFlowing(this.totalFlowingB.getAndSet(0));
            slotsBillInfo.setTotalIncome(this.totalIncomeB.getAndSet(0));

            if(!this.partInPlayerBetB.isEmpty()){
                slotsBillInfo.setPartInPlayerBet(new HashMap<>(this.partInPlayerBetB));
            }
            this.partInPlayerBetB.clear();

            if(!this.partInPlayerIncomeB.isEmpty()){
                slotsBillInfo.setPartInPlayerIncome(new HashMap<>(this.partInPlayerIncomeB));
            }
            this.partInPlayerIncomeB.clear();
        }
        return slotsBillInfo;
    }
}
