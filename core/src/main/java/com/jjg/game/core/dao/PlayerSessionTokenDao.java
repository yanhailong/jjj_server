package com.jjg.game.core.dao;

import com.mongodb.client.result.DeleteResult;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.data.PlayerSessionToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/5/26 10:14
 */
@Repository
public class PlayerSessionTokenDao extends MongoBaseDao<PlayerSessionToken, Long> {

    public PlayerSessionTokenDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerSessionToken.class, mongoTemplate);
    }

    //token过期时长
    private final long tokenExpireTime = 2400L * TimeHelper.ONE_HOUR_OF_MILLIS;

    /**
     * 保存token
     *
     * @param token
     * @param playerId
     */
    public void save(String token, int loginType, long playerId, int channel) {
        PlayerSessionToken playerSessionToken = new PlayerSessionToken();
        playerSessionToken.setPlayerId(playerId);
        playerSessionToken.setToken(token);
        playerSessionToken.setExpireTime(System.currentTimeMillis() + tokenExpireTime);
        playerSessionToken.setLoginType(loginType);
        playerSessionToken.setChannel(channel);
        mongoTemplate.save(playerSessionToken);
    }

    /**
     * 获取db中session信息
     *
     * @param playerId
     * @return
     */
    public PlayerSessionToken getByPlayerId(long playerId) {
        return mongoTemplate.findById(playerId, PlayerSessionToken.class);
    }

    /**
     * 清除过期token
     */
    public DeleteResult clearExpireToken() {
        Query query = new Query(Criteria.where("expireTime").lt(System.currentTimeMillis()));
        // 执行删除
        return mongoTemplate.remove(query, PlayerSessionToken.class);
    }
}
