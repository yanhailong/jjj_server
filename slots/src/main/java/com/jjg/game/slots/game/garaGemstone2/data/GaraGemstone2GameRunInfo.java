package com.jjg.game.slots.game.garaGemstone2.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.garaGemstone2.pb.GaraGemstone2WinIconInfo;

import java.util.List;

public class GaraGemstone2GameRunInfo extends GameRunInfo<GaraGemstone2PlayerGameData> {

    private List<GaraGemstone2WinIconInfo> awardLineInfos;
    /** 第四轴（倍数轴）中间格子的倍数值 */
    private long multiplyAxisTimes = 1;
    /** 第四轴转出wheel的额外倍数 */
    private long wheelTimes = 0;

    public GaraGemstone2GameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<GaraGemstone2WinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<GaraGemstone2WinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public long getMultiplyAxisTimes() {
        return multiplyAxisTimes;
    }

    public void setMultiplyAxisTimes(long multiplyAxisTimes) {
        this.multiplyAxisTimes = multiplyAxisTimes;
    }

    public long getWheelTimes() {
        return this.wheelTimes;
    }

    public void setWheelTimes(long wheelTimes) {
        this.wheelTimes = wheelTimes;
    }
}
