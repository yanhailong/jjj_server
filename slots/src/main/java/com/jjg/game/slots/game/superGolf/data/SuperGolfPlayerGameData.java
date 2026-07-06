package com.jjg.game.slots.game.superGolf.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 超级高尔夫玩家游戏数据。
 * <p>
 * {@code freeMultiplierAccum} 是 SuperGolf 特有的字段：免费模式下从神秘符号收集的乘倍值，
 * 跨 spin 累计直到免费结束（文档 [42]）。普通模式不用，每 spin 单独算。
 */
@Document
public class SuperGolfPlayerGameData extends SlotsPlayerGameData {
    //免费模式累计倍率，免费第一局开始时由 manager 重置为 0
    private int freeMultiplierAccum;

    public int getFreeMultiplierAccum() {
        return freeMultiplierAccum;
    }

    public void setFreeMultiplierAccum(int freeMultiplierAccum) {
        this.freeMultiplierAccum = freeMultiplierAccum;
    }
}
