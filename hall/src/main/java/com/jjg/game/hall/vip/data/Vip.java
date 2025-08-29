package com.jjg.game.hall.vip.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lm
 * @date 2025/8/27 10:03
 */
@Document
public class Vip {
    //玩家id
    @Id
    private long playerId;
    //礼包类型 领取时间
    private Map<Integer, Long> giftGetTime;
    //vip等级礼包 领取时间
    private Map<Integer, Long> lvGiftGetTime;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Map<Integer, Long> getGiftGetTime() {
        return giftGetTime;
    }

    public void setGiftGetTime(Map<Integer, Long> giftGetTime) {
        this.giftGetTime = giftGetTime;
    }

    public Map<Integer, Long> getLvGiftGetTime() {
        return lvGiftGetTime;
    }

    public void setLvGiftGetTime(Map<Integer, Long> lvGiftGetTime) {
        this.lvGiftGetTime = lvGiftGetTime;
    }

    public static Vip buildVip(long playerId) {
        Vip vip = new Vip();
        vip.giftGetTime = new HashMap<>();
        vip.lvGiftGetTime = new HashMap<>();
        vip.playerId = playerId;
        return vip;
    }
}
