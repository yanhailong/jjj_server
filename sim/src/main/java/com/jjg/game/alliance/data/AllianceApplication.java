package com.jjg.game.alliance.data;

/**
 * 入盟申请条目 (内嵌于 {@link AllianceData#getApplications()})。
 * <p>
 * 赌场等级在申请时刻快照, 申请列表展示直接用, 免去逐人回查赌场数据。
 *
 * @author 11
 * @date 2026/6/11
 */
public class AllianceApplication {
    //申请时间(ms)
    private long applyTime;
    //申请时的赌场等级快照
    private int casinoLevel;

    public AllianceApplication() {
    }

    public AllianceApplication(long applyTime, int casinoLevel) {
        this.applyTime = applyTime;
        this.casinoLevel = casinoLevel;
    }

    public long getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(long applyTime) {
        this.applyTime = applyTime;
    }

    public int getCasinoLevel() {
        return casinoLevel;
    }

    public void setCasinoLevel(int casinoLevel) {
        this.casinoLevel = casinoLevel;
    }
}
