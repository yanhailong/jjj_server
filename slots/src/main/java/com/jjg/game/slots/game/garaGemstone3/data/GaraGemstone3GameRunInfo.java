package com.jjg.game.slots.game.garaGemstone3.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.garaGemstone3.pb.GaraGemstone3WinIconInfo;

import java.util.List;

public class GaraGemstone3GameRunInfo extends GameRunInfo<GaraGemstone3PlayerGameData> {

    private List<GaraGemstone3WinIconInfo> awardLineInfos;
    /** 第四轴（倍数轴）中间格子的倍数值 */
    private long multiplyAxisTimes = 1;

    public GaraGemstone3GameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<GaraGemstone3WinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<GaraGemstone3WinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public long getMultiplyAxisTimes() {
        return multiplyAxisTimes;
    }

    public void setMultiplyAxisTimes(long multiplyAxisTimes) {
        this.multiplyAxisTimes = multiplyAxisTimes;
    }
}
