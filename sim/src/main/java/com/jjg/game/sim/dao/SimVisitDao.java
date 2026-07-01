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

    public void clearUnreadComments(long playerId, int readCount, String latestCommentId) {
        if (readCount <= 0 || latestCommentId == null) {
            return;
        }
        //CAS 同时校验未读数和最新留言，避免并发读取误清刚到达的新留言。
        Query query = Query.query(Criteria.where("_id").is(playerId)
                .and("unreadCommentCount").is(readCount)
                .and("comments.0.id").is(latestCommentId));
        mongoTemplate.updateFirst(query,
                new Update().set("unreadCommentCount", 0), SimVisitProfileData.class);
    }

    public List<SimVisitProfileData> findAllByPlayerIds(Collection<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }
        Query query = Query.query(Criteria.where("_id").in(playerIds));
        query.fields().include("playerId", "totalPopularity", "unreadCommentCount");
        return mongoTemplate.find(query, SimVisitProfileData.class);
    }
}
