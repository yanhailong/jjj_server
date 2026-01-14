package com.jjg.game.core.dao;

import com.jjg.game.core.data.ChannelType;
import com.jjg.game.core.data.OnlinePlayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/27 17:07
 */
@Repository
public class OnlinePlayerDao extends MongoBaseDao<OnlinePlayer, Long> {
    public OnlinePlayerDao(@Autowired MongoTemplate mongoTemplate) {
        super(OnlinePlayer.class, mongoTemplate);
    }

    public void online(long playerId, int channel, int gameType, int roomCfgId, String subChannel) {
        OnlinePlayer onlinePlayer = new OnlinePlayer();
        onlinePlayer.setPlayerId(playerId);
        onlinePlayer.setChannel(channel);
        onlinePlayer.setGameType(gameType);
        onlinePlayer.setRoomCfgId(roomCfgId);
        onlinePlayer.setSubChannel(subChannel);
        mongoTemplate.save(onlinePlayer);
    }

    public void changeGameType(long playerId, int gameType, int roomCfgId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        Update update = new Update();
        update.set("gameType", gameType);
        update.set("roomCfgId", roomCfgId);
        mongoTemplate.updateFirst(query, update, OnlinePlayer.class);
    }

    public void delete(long playerId) {
        mongoTemplate.remove(new Query(Criteria.where("playerId").is(playerId)), OnlinePlayer.class);
    }

    public void delete(List<Long> playerIds) {
        mongoTemplate.remove(new Query(Criteria.where("playerId").in(playerIds)), OnlinePlayer.class);
    }

    public List<OnlinePlayer> query(int gameType, int channel, List<String> subChannels, int pageSize, int page) {
        if (pageSize > 100) {
            pageSize = 100;
        }

        page = page - 1;
        if (page < 0) {
            page = 0;
        }

        Criteria criteria = Criteria.where("gameType").is(gameType);

        ChannelType channelType = ChannelType.valueOf(channel, null);
        if (channelType != null) {
            criteria.and("channel").is(channelType.getValue());
        }

        if (subChannels != null && !subChannels.isEmpty()) {
            criteria.and("subChannel").in(subChannels);
        }

        Query query = Query.query(criteria)
                .skip((long) page * pageSize)
                .limit(pageSize);

        return mongoTemplate.find(query, OnlinePlayer.class);
    }

    public long countBy(int channel, int gameType) {
        Query query = new Query();
        if (channel > 0) {
            query.addCriteria(Criteria.where("channel").is(channel).and("gameType").is(gameType));
        } else {
            query.addCriteria(Criteria.where("gameType").is(gameType));
        }
        return mongoTemplate.count(query, OnlinePlayer.class);
    }
}