package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.slots.data.PlayerHistorySlots;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/7/11 10:28
 */
@Repository
public class PlayerHistorySlotsDao extends MongoBaseDao<PlayerHistorySlots,Long> {
    public PlayerHistorySlotsDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerHistorySlots.class, mongoTemplate);
    }

    /**
     * 添加玩家玩过的游戏类型
     * @param playerId
     * @param gameType
     */
    public void addGameType(long playerId, int gameType) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        Update update = new Update().addToSet("slots", gameType);

        mongoTemplate.updateFirst(query,update,PlayerHistorySlots.class);
    }

    /**
     * 检查玩家是否玩过某个slots游戏
     * @param playerId
     * @param gameType
     * @return
     */
    public boolean hasPlaySlots(long playerId, int gameType){
        Query query = new Query(Criteria.where("playerId").is(playerId).and("slots").in(gameType));
        // 检查是否存在匹配的文档
        return mongoTemplate.exists(query, PlayerHistorySlots.class);
    }
}
