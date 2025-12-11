package com.jjg.game.slots.game.captainjack.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameData;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 11
 * @date 2025/8/1 17:55
 */
public class CaptainJackGameRunInfo extends GameRunInfo<CaptainJackPlayerGameData> {

    public CaptainJackGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    //已经探宝次数
    private int alreadyDigCount;
    //累计探宝倍率
    private int digTimesMultiplier;

    public int getAlreadyDigCount() {
        return alreadyDigCount;
    }

    public void setAlreadyDigCount(int alreadyDigCount) {
        this.alreadyDigCount = alreadyDigCount;
    }

    public int getDigTimesMultiplier() {
        return digTimesMultiplier;
    }

    public void addDigTimesMultiplier(int addValue) {
         digTimesMultiplier += addValue;
    }

}
