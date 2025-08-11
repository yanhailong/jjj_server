package com.jjg.game.core.dao;

import com.jjg.game.core.data.PlayerAvatar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/7 16:32
 */
@Repository
public class PlayerAvatarDao extends MongoBaseDao<PlayerAvatar,Long>{
    public PlayerAvatarDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerAvatar.class, mongoTemplate);
    }

    /**
     * 添加头像
     * @param playerId
     * @param avatarId
     */
    public boolean addAvatar(long playerId,int avatarId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        Update update = new Update().addToSet("unlockAvatarSet", avatarId);
        return mongoTemplate.upsert(query, update, PlayerAvatar.class).wasAcknowledged();
    }

    /**
     * 是否拥有该头像
     * @param playerId
     * @param avatarId
     */
    public boolean hasAvatar(long playerId,int avatarId) {
        Query query = new Query(Criteria.where("playerId").is(playerId).and("unlockAvatarSet").in(avatarId));
        return mongoTemplate.exists(query, PlayerAvatar.class);
    }

    /**
     * 添加头像框
     * @param playerId
     * @param frameId
     */
    public boolean addFrame(long playerId,int frameId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        Update update = new Update().addToSet("unlockFrameSet", frameId);
        return mongoTemplate.upsert(query, update, PlayerAvatar.class).wasAcknowledged();
    }

    /**
     * 是否拥有该头像框
     * @param playerId
     * @param frameId
     */
    public boolean hasFrame(long playerId,int frameId) {
        Query query = new Query(Criteria.where("playerId").is(playerId).and("unlockFrameSet").in(frameId));
        return mongoTemplate.exists(query, PlayerAvatar.class);
    }

    /**
     * 添加称号
     * @param playerId
     * @param titleId
     */
    public boolean addTitle(long playerId,int titleId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        Update update = new Update().addToSet("unlockTitleSet", titleId);
        return mongoTemplate.upsert(query, update, PlayerAvatar.class).wasAcknowledged();
    }

    /**
     * 是否拥有该称号
     * @param playerId
     * @param titleId
     */
    public boolean hasTitle(long playerId,int titleId) {
        Query query = new Query(Criteria.where("playerId").is(playerId).and("unlockTitleSet").in(titleId));
        return mongoTemplate.exists(query, PlayerAvatar.class);
    }
}
