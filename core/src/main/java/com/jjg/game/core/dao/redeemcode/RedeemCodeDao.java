package com.jjg.game.core.dao.redeemcode;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.core.data.RedeemCode;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * @author lm
 * @date 2026/3/12 15:40
 */
@Repository
public class RedeemCodeDao extends MongoBaseDao<RedeemCode, String> {
    public RedeemCodeDao(MongoTemplate mongoTemplate) {
        super(RedeemCode.class, mongoTemplate);
    }

    public RedeemCode add(RedeemCode redeemCode) {
        return mongoTemplate.insert(redeemCode);
    }

    public Collection<RedeemCode> addAll(Collection<RedeemCode> redeemCodes) {
        return mongoTemplate.insert(redeemCodes, clazz);
    }

    public boolean queryRedeemCodesByRedeemIdAndUsePlayerId(long redeemId, long usePlayerId) {
        Query query = Query.query(Criteria.where("usePlayerId").is(usePlayerId).and("redeemId").is(redeemId));
        return mongoTemplate.exists(query, clazz);
    }

    public boolean updateUsePlayerIdAndUseTime(String code, long playerId) {
        Query query = Query.query(
                Criteria.where("_id").is(code)
                        .and("usePlayerId").is(0)
                        .and("useTime").is(0));
        Update update = new Update().set("usePlayerId", playerId)
                .set("useTime", System.currentTimeMillis());
        UpdateResult result = mongoTemplate.updateFirst(query, update, clazz);
        return result.getModifiedCount() > 0;
    }

    public boolean rollBackUsePlayerIdAndUseTime(String code, long playerId) {
        Query query = Query.query(
                Criteria.where("_id").is(code)
                        .and("usePlayerId").is(playerId));
        Update update = new Update().set("usePlayerId", 0)
                .set("useTime", 0);
        UpdateResult result = mongoTemplate.updateFirst(query, update, clazz);
        return result.getModifiedCount() > 0;
    }
}
