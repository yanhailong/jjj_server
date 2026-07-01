package com.jjg.game.sim.data;

/**
 * 房主最近拜访操作记录。
 *
 * @author 11
 * @date 2026/6/30
 */
public class SimVisitRecordData {
    private String id;
    private long visitorId;
    private String visitorName;
    private int headImgId;
    private int headFrameId;
    private int casinoId;
    private int type;
    private int gameType;
    private int giftId;
    private int popularity;
    private long commissionGold;
    private long createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getVisitorId() { return visitorId; }
    public void setVisitorId(long visitorId) { this.visitorId = visitorId; }
    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }
    public int getHeadImgId() { return headImgId; }
    public void setHeadImgId(int headImgId) { this.headImgId = headImgId; }
    public int getHeadFrameId() { return headFrameId; }
    public void setHeadFrameId(int headFrameId) { this.headFrameId = headFrameId; }
    public int getCasinoId() { return casinoId; }
    public void setCasinoId(int casinoId) { this.casinoId = casinoId; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public int getGameType() { return gameType; }
    public void setGameType(int gameType) { this.gameType = gameType; }
    public int getGiftId() { return giftId; }
    public void setGiftId(int giftId) { this.giftId = giftId; }
    public int getPopularity() { return popularity; }
    public void setPopularity(int popularity) { this.popularity = popularity; }
    public long getCommissionGold() { return commissionGold; }
    public void setCommissionGold(long commissionGold) { this.commissionGold = commissionGold; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
