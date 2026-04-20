package com.jjg.game.slots.game.garaGemstone1.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.garaGemstone1.pb.GaraGemstone1WinIconInfo;

import java.util.List;

public class GaraGemstone1GameRunInfo extends GameRunInfo<GaraGemstone1PlayerGameData> {

    private List<GaraGemstone1WinIconInfo> awardLineInfos;
    /** 第四轴（倍数轴）中间格子的倍数值 */
    private long multiplyAxisTimes = 1;

    public GaraGemstone1GameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<GaraGemstone1WinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<GaraGemstone1WinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public long getMultiplyAxisTimes() {
        return multiplyAxisTimes;
    }

    public void setMultiplyAxisTimes(long multiplyAxisTimes) {
        this.multiplyAxisTimes = multiplyAxisTimes;
    }
}
