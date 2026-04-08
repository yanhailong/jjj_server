package com.jjg.game.slots.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 存储一些玩家在所有slots游戏中的一些数据
 *
 * @author 11
 * @date 2026/4/1
 */
@Document
public class PlayerAllSlotsData {
    @Id
    private long playerId;
    //累计押注次数
    private int allBetCount;
    //连续未中奖次数
    private int prizelessCount;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getAllBetCount() {
        return allBetCount;
    }

    public void setAllBetCount(int allBetCount) {
        this.allBetCount = allBetCount;
    }

    public int getPrizelessCount() {
        return prizelessCount;
    }

    public void setPrizelessCount(int prizelessCount) {
        this.prizelessCount = prizelessCount;
    }

    /**
     * 增加下注次数
     */
    public void incrementBetCount() {
        this.allBetCount ++;
    }

    public void updatePrizelessCount(long win) {
        if(win < 1){
            this.prizelessCount ++;
        }else {
            this.prizelessCount = 0;
        }
    }
}
