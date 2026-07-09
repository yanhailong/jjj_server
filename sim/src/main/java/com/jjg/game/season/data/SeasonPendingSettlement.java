package com.jjg.game.season.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 跨节点异步对局的待结算记录。记录独立于玩家聚合，避免其他节点的旧内存快照覆盖结算结果。
 * <p>
 * {@code createTime} 为 BSON Date，配合 TTL 索引由 Mongo 自动清理长期未消费的记录
 * （对手切季后旧 seasonKey 的记录不再可消费，靠切季清理 + TTL 兜底回收）。
 */
@Document
public class SeasonPendingSettlement {
    @Id
    private String id;
    private long playerId;
    private String seasonKey;
    private String matchId;
    private long coinDelta;
    private SeasonMatchRecord record;
    private int historyLimit;
    private Date createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }
    public String getSeasonKey() { return seasonKey; }
    public void setSeasonKey(String seasonKey) { this.seasonKey = seasonKey; }
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public long getCoinDelta() { return coinDelta; }
    public void setCoinDelta(long coinDelta) { this.coinDelta = coinDelta; }
    public SeasonMatchRecord getRecord() { return record; }
    public void setRecord(SeasonMatchRecord record) { this.record = record; }
    public int getHistoryLimit() { return historyLimit; }
    public void setHistoryLimit(int historyLimit) { this.historyLimit = historyLimit; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
