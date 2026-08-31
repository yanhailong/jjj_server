package com.jjg.game.sim.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.data.SimVisitCommentData;
import com.jjg.game.sim.data.SimVisitProfileData;
import com.jjg.game.sim.data.SimVisitRecordData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 拜访数据原子更新入口。
 *
 * @author 11
 * @date 2026/6/30
 */
@Repository
public class SimVisitDao extends MongoBaseDao<SimVisitProfileData, Long> {
    public SimVisitDao(@Autowired MongoTemplate mongoTemplate) {
        super(SimVisitProfileData.class, mongoTemplate);
    }

    public SimVisitProfileData addInteraction(long playerId, int popularity,
                                              SimVisitRecordData record,
                                              SimVisitCommentData comment,
                                              int limit) {
        Query query = Query.query(Criteria.where("_id").is(playerId));
        Update update = new Update();
        if (popularity > 0) {
            update.inc("totalPopularity", popularity);
        }
        if (record != null) {
            update.push("records").atPosition(0).slice(limit).each(record);
        }
        if (comment != null) {
            comment.setUnread(true);
            update.push("comments").atPosition(0).slice(limit).each(comment);
            update.inc("unreadCommentCount", 1);
            update.inc("commentRevision", 1);
        }
        SimVisitProfileData result = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().upsert(true).returnNew(true), SimVisitProfileData.class);
        // slice淘汰的留言不再计入未读。
        return comment == null ? result : editComments(playerId, java.util.Set.of(), null);
    }

    public boolean deleteComment(long playerId, String commentId) {
        SimVisitProfileData current = findCommentsView(playerId);
        if (current == null || current.getComments().stream().noneMatch(c -> commentId.equals(c.getId()))) return false;
        editComments(playerId, java.util.Set.of(), commentId);
        return true;
    }

    public SimVisitProfileData markCommentsRead(long playerId, Collection<String> ids) {
        return editComments(playerId, new java.util.HashSet<>(ids), null);
    }

    /** 更新留言状态及计数为同一次原子写入；CAS失败重新读取，不覆盖新留言。 */
    private SimVisitProfileData editComments(long playerId, java.util.Set<String> readIds, String deleteId) {
        for (int attempt = 0; attempt < 8; attempt++) {
            SimVisitProfileData profile = findCommentsView(playerId);
            if (profile == null) return null;
            int oldCount = profile.getUnreadCommentCount();
            java.util.List<SimVisitCommentData> comments = new java.util.ArrayList<>(profile.getComments());
            normalizeUnread(comments, oldCount);
            comments.removeIf(c -> deleteId != null && deleteId.equals(c.getId()));
            comments.forEach(c -> { if (readIds.contains(c.getId())) c.setUnread(false); });
            int count = (int) comments.stream().filter(c -> Boolean.TRUE.equals(c.getUnread())).count();
            Query query = Query.query(Criteria.where("_id").is(playerId)
                    .and("commentRevision").is(profile.getCommentRevision()));
            Update update = new Update().set("comments", comments).set("unreadCommentCount", count)
                    .inc("commentRevision", 1);
            SimVisitProfileData updated = mongoTemplate.findAndModify(query, update,
                    FindAndModifyOptions.options().returnNew(true), SimVisitProfileData.class);
            if (updated != null) return updated;
        }
        throw new IllegalStateException("留言状态并发修改重试失败 playerId=" + playerId);
    }

    /** 旧逻辑一次清空，故旧记录中最新的N条就是未读；已迁移记录保留逐条状态。 */
    static void normalizeUnread(List<SimVisitCommentData> comments, int oldCount) {
        for (int i = 0; i < comments.size(); i++) {
            if (comments.get(i).getUnread() == null) comments.get(i).setUnread(i < oldCount);
        }
    }

    public List<SimVisitProfileData> findAllByPlayerIds(Collection<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }
        Query query = Query.query(Criteria.where("_id").in(playerIds));
        query.fields().include("playerId", "totalPopularity", "unreadCommentCount");
        return mongoTemplate.find(query, SimVisitProfileData.class);
    }

    /**
     * 概要投影 (不含记录/留言数组), 只读人气汇总时避免拉全量文档。
     */
    public SimVisitProfileData findBrief(long playerId) {
        Query query = Query.query(Criteria.where("_id").is(playerId));
        query.fields().include("playerId", "totalPopularity", "unreadCommentCount");
        return mongoTemplate.findOne(query, SimVisitProfileData.class);
    }

    /**
     * 记录页视图: 排除留言数组, 翻页时少拉一半内嵌数据。
     */
    public SimVisitProfileData findRecordsView(long playerId) {
        Query query = Query.query(Criteria.where("_id").is(playerId));
        query.fields().exclude("comments");
        return mongoTemplate.findOne(query, SimVisitProfileData.class);
    }

    /**
     * 留言页视图: 排除拜访记录数组。
     */
    public SimVisitProfileData findCommentsView(long playerId) {
        Query query = Query.query(Criteria.where("_id").is(playerId));
        query.fields().exclude("records");
        return mongoTemplate.findOne(query, SimVisitProfileData.class);
    }
}
