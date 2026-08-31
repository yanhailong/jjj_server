package com.jjg.game.sim.data;

/**
 * 房主留言板记录。
 *
 * @author 11
 * @date 2026/6/30
 */
public class SimVisitCommentData {
    private String id;
    private long visitorId;
    private String visitorName;
    private int headImgId;
    private int headFrameId;
    private int casinoId;
    private String content;
    private int popularity;
    private long createTime;
    // null表示旧版本数据，首次访问时依据旧的未读计数兼容迁移。
    private Boolean unread;
    public Boolean getUnread() { return unread; }
    public void setUnread(Boolean unread) { this.unread = unread; }

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
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getPopularity() { return popularity; }
    public void setPopularity(int popularity) { this.popularity = popularity; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
