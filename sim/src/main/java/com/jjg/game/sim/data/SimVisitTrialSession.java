package com.jjg.game.sim.data;

/**
 * Redis 中的客座赌局会话。
 *
 * @author 11
 * @date 2026/6/30
 */
public class SimVisitTrialSession {
    private String sessionId;
    private long visitorId;
    private long ownerId;
    private int casinoId;
    private int gameType;
    private long expireTime;

    public SimVisitTrialSession() {
    }

    public SimVisitTrialSession(String sessionId, long visitorId, long ownerId,
                                int casinoId, int gameType, long expireTime) {
        this.sessionId = sessionId;
        this.visitorId = visitorId;
        this.ownerId = ownerId;
        this.casinoId = casinoId;
        this.gameType = gameType;
        this.expireTime = expireTime;
    }

    public boolean matches(long visitorId, long ownerId, int casinoId, int gameType, long now) {
        return this.visitorId == visitorId
                && this.ownerId == ownerId
                && this.casinoId == casinoId
                && this.gameType == gameType
                && expireTime >= now;
    }

    public boolean activeFor(long visitorId, int gameType, long now) {
        return this.visitorId == visitorId && this.gameType == gameType && expireTime >= now;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public long getVisitorId() { return visitorId; }
    public void setVisitorId(long visitorId) { this.visitorId = visitorId; }
    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }
    public int getCasinoId() { return casinoId; }
    public void setCasinoId(int casinoId) { this.casinoId = casinoId; }
    public int getGameType() { return gameType; }
    public void setGameType(int gameType) { this.gameType = gameType; }
    public long getExpireTime() { return expireTime; }
    public void setExpireTime(long expireTime) { this.expireTime = expireTime; }
}
