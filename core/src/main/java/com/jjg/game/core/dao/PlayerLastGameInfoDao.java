package com.jjg.game.core.dao;

import com.jjg.game.core.data.PlayerLastGameInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 11
 * @date 2025/5/26 17:03
 */
@Repository
public class PlayerLastGameInfoDao extends MongoBaseDao<PlayerLastGameInfo, Long>{
    public PlayerLastGameInfoDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerLastGameInfo.class, mongoTemplate);
    }

    public List<PlayerLastGameInfo> queryPlayerLastGameInfos(int pageSize, int page){
        Query query = new Query();
        int skip = page * pageSize;
        query.skip(skip);
        //query.with(Sort.by(Sort.Order.desc("_id")));
        query.limit(pageSize);
        return mongoTemplate.find(query, PlayerLastGameInfo.class);
    }

    public void resetPlayerGameInfo(Long playerId){
        Query query = new Query(Criteria.where("_id").is(playerId));
        Update update = new Update();
        update.set("halfwayOffline",false);
        update.set("canExit",true);
        mongoTemplate.updateFirst(query,update,PlayerLastGameInfo.class);
    }
}
