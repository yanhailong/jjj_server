package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家拜访持久数据。与 SimPlayerContext 分离，避免跨节点互动被房主自动存档覆盖。
 *
 * @author 11
 * @date 2026/6/30
 */
@Document
public class SimVisitProfileData {
    @Id
    private long playerId;
    private long totalPopularity;
    private int unreadCommentCount;
    // 留言红点CAS版本，防止分页已读覆盖并发到达的新留言。
    private Long commentRevision;
    public Long getCommentRevision() { return commentRevision; }
    public void setCommentRevision(Long commentRevision) { this.commentRevision = commentRevision; }
    private List<SimVisitRecordData> records;
    private List<SimVisitCommentData> comments;

    public void addRecord(SimVisitRecordData record, int limit) {
        if (record == null || limit <= 0) {
            return;
        }
        if (records == null) {
            records = new ArrayList<>(Math.min(limit, 16));
        }
        records.add(0, record);
        if (records.size() > limit) {
            records.subList(limit, records.size()).clear();
        }
    }

    public void addComment(SimVisitCommentData comment, int limit) {
        if (comment == null || limit <= 0) {
            return;
        }
        if (comments == null) {
            comments = new ArrayList<>(Math.min(limit, 16));
        }
        comments.add(0, comment);
        if (comments.size() > limit) {
            comments.subList(limit, comments.size()).clear();
        }
        unreadCommentCount++;
    }

    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }
    public long getTotalPopularity() { return totalPopularity; }
    public void setTotalPopularity(long totalPopularity) { this.totalPopularity = totalPopularity; }
    public int getUnreadCommentCount() { return unreadCommentCount; }
    public void setUnreadCommentCount(int unreadCommentCount) { this.unreadCommentCount = unreadCommentCount; }
    public List<SimVisitRecordData> getRecords() {
        return records == null ? List.of() : records;
    }
    public void setRecords(List<SimVisitRecordData> records) { this.records = records; }
    public List<SimVisitCommentData> getComments() {
        return comments == null ? List.of() : comments;
    }
    public void setComments(List<SimVisitCommentData> comments) { this.comments = comments; }
}
