package com.jjg.game.activepass.dao;

import com.jjg.game.activepass.data.ActivePassData;
import com.jjg.game.core.dao.MongoBaseDao;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class ActivePassDao extends MongoBaseDao<ActivePassData, String> {
    public ActivePassDao(MongoTemplate mongoTemplate) {
        super(ActivePassData.class, mongoTemplate);
        mongoTemplate.indexOps(ActivePassData.class).ensureIndex(new Index()
                .on("playerId", Sort.Direction.ASC).on("settled", Sort.Direction.ASC).on("endTime", Sort.Direction.ASC));
    }
    public List<ActivePassData> expired(long playerId, long now) {
        return mongoTemplate.find(Query.query(Criteria.where("playerId").is(playerId)
                .and("settled").is(false).and("endTime").lte(now)), ActivePassData.class);
    }
}
