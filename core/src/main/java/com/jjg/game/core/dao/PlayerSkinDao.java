package com.jjg.game.core.dao;

import com.jjg.game.core.data.AvatarType;
import com.jjg.game.core.data.PlayerSkin;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/8/7 16:32
 */
@Repository
public class PlayerSkinDao extends MongoBaseDao<PlayerSkin, Long> {

    private final Logger log = LoggerFactory.getLogger(PlayerSkinDao.class);

    public PlayerSkinDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerSkin.class, mongoTemplate);
    }

    /**
     * 获取玩家头像信息
     *
     * @param playerId
     * @return
     */
    public PlayerSkin getPlayerSkin(long playerId) {
        return mongoTemplate.findById(playerId, PlayerSkin.class);
    }

    /**
     * 添加数据
     *
     * @param playerId 玩家id
     * @param type     数据类型
     * @param id       数据id
     * @return 是否添加成功
     */
    public boolean addByType(long playerId, AvatarType type, int id) {
        if (StringUtils.isEmpty(type.getField())) {
            return false;
        }
        Query query = new Query(Criteria.where("playerId").is(playerId));
        Update update = new Update().addToSet(type.getField(), id);
        UpdateResult result = mongoTemplate.upsert(query, update, PlayerSkin.class);
        return result.getModifiedCount() > 0 || result.getUpsertedId() != null;
    }

    /**
     * 添加数据
     * @param playerId
     * @param addIdsMap
     * @return
     */
    public boolean addByType(long playerId, Map<AvatarType, List<Integer>> addIdsMap) {
        if(addIdsMap == null || addIdsMap.isEmpty()) {
            return false;
        }

        Query query = new Query(Criteria.where("playerId").is(playerId));

        Update update = new Update();

        boolean flag = false;
        for (Map.Entry<AvatarType, List<Integer>> entry : addIdsMap.entrySet()) {
            AvatarType type = entry.getKey();
            List<Integer> ids = entry.getValue();

            // 跳过字段名为空或id列表为空的情况
            if (StringUtils.isEmpty(type.getField()) || ids == null || ids.isEmpty()) {
                continue;
            }

            update.addToSet(type.getField()).each(ids.toArray());
            flag = true;
        }

        if (!flag) {
            return false;
        }

        UpdateResult result = mongoTemplate.upsert(query, update, PlayerSkin.class);
        return result.getModifiedCount() > 0 || result.getUpsertedId() != null;
    }

    /**
     * 是否拥有数据
     *
     * @param playerId 玩家id
     * @param type     数据类型
     * @return 是否拥有
     */
    public boolean hasByType(long playerId, AvatarType type, int id) {
        if (StringUtils.isEmpty(type.getField())) {
            return false;
        }
        Query query = new Query(Criteria.where("playerId").is(playerId).and(type.getField()).in(id));
        return mongoTemplate.exists(query, PlayerSkin.class);
    }


}
