package com.jjg.game.sim.data;

/**
 * slots 试玩旋转许可。trial=false 表示普通旋转。
 *
 * @author 11
 * @date 2026/6/30
 */
public class VisitTrialSpinPermit {
    private boolean trial;
    private String permitId;
    private long ownerId;
    private int casinoId;
    private int remainingCount;
    private int power;

    public boolean isTrial() { return trial; }
    public void setTrial(boolean trial) { this.trial = trial; }
    public String getPermitId() { return permitId; }
    public void setPermitId(String permitId) { this.permitId = permitId; }
    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }
    public int getCasinoId() { return casinoId; }
    public void setCasinoId(int casinoId) { this.casinoId = casinoId; }
    public int getRemainingCount() { return remainingCount; }
    public void setRemainingCount(int remainingCount) { this.remainingCount = remainingCount; }
    public int getPower() { return power; }
    public void setPower(int power) { this.power = power; }
}
