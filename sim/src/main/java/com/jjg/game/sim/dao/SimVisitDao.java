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
            update.push("comments").atPosition(0).slice(limit).each(comment);
            update.inc("unreadCommentCount", 1);
        }
        return mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().upsert(true).returnNew(true), SimVisitProfileData.class);
    }

    public boolean deleteComment(long playerId, String commentId) {
        Query owner = Query.query(Criteria.where("_id").is(playerId)
                .and("comments.id").is(commentId));
        Query comment = Query.query(Criteria.where("id").is(commentId));
        return mongoTemplate.updateFirst(owner,
                new Update().pull("comments", comment.getQueryObject()), SimVisitProfileData.class)
                .getModifiedCount() > 0;
    }

    public boolean clearUnreadComments(long playerId, int readCount, String latestCommentId) {
        if (readCount <= 0 || latestCommentId == null) {
            return false;
        }
        //CAS 同时校验未读数和最新留言，避免并发读取误清刚到达的新留言。
        Query query = Query.query(Criteria.where("_id").is(playerId)
                .and("unreadCommentCount").is(readCount)
                .and("comments.0.id").is(latestCommentId));
        return mongoTemplate.updateFirst(query,
                new Update().set("unreadCommentCount", 0), SimVisitProfileData.class)
                .getModifiedCount() > 0;
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
