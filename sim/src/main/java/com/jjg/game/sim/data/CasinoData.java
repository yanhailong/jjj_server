package com.jjg.game.sim.data;

/**
 * 赌场信息
 * @author 11
 * @date 2026/5/21
 */
public class CasinoData {    //赌场id
    private int id;
    //当前繁荣度
    private int prosperity;
    //曝光结束时间(ms)，0 表示未曝光
    private long exposureEndTime;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProsperity() {
        return prosperity;
    }

    public void setProsperity(int prosperity) {
        this.prosperity = prosperity;
    }

    public long getExposureEndTime() {
        return exposureEndTime;
    }

    public void setExposureEndTime(long exposureEndTime) {
        this.exposureEndTime = exposureEndTime;
    }
}
