package com.jjg.game.slots.game.steamAge.data;

import com.jjg.game.slots.data.FullAwardLineInfo;

/**
 * @author lihaocao
 * @date 2025/12/2 17:31
 */
public class SteamAgeAwardLineInfo extends FullAwardLineInfo {

    //线倍数
    private int lineTimes;
    //总倍数 线倍数*连续倍数
    private int totalTimes;

    public int getTotalTimes() {
        return totalTimes;
    }

    public void setTotalTimes(int totalTimes) {
        this.totalTimes = totalTimes;
    }

    public int getLineTimes() {
        return lineTimes;
    }

    public void setLineTimes(int lineTimes) {
        this.lineTimes = lineTimes;
    }

}
