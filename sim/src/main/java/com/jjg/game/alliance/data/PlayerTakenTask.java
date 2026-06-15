package com.jjg.game.alliance.data;

/**
 * 玩家当前接取的联盟任务快照 (内嵌于 {@link AlliancePlayerData})。
 * <p>
 * 仅存接取时刻的任务快照(单次只能接一条); 进度计数走 Redis (高频事件上报不打 Mongo),
 * 完成/放弃/超期时清除本快照。
 *
 * @author 11
 * @date 2026/6/11
 */
public class PlayerTakenTask {
    //任务实例 uid
    private long uid;
    //任务配置 id
    private int cfgId;
    //接取时所在联盟 (完成时声誉入账给该联盟)
    private long allianceId;
    //接取时间(ms)
    private long acceptTime;
    //截止时间(ms): 超期视为失败自动放弃
    private long expireTime;

    public PlayerTakenTask() {
    }

    public PlayerTakenTask(long uid, int cfgId, long allianceId, long acceptTime, long expireTime) {
        this.uid = uid;
        this.cfgId = cfgId;
        this.allianceId = allianceId;
        this.acceptTime = acceptTime;
        this.expireTime = expireTime;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public int getCfgId() {
        return cfgId;
    }

    public void setCfgId(int cfgId) {
        this.cfgId = cfgId;
    }

    public long getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(long allianceId) {
        this.allianceId = allianceId;
    }

    public long getAcceptTime() {
        return acceptTime;
    }

    public void setAcceptTime(long acceptTime) {
        this.acceptTime = acceptTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public boolean expired(long now) {
        return expireTime > 0 && now > expireTime;
    }
}
