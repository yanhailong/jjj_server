package com.jjg.game.core.dao;

import cn.hutool.core.util.EnumUtil;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.AvatarType;
import com.jjg.game.core.data.PlayerAvatar;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.AvatarCfg;
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

/**
 * @author 11
 * @date 2025/8/7 16:32
 */
@Repository
public class PlayerAvatarDao extends MongoBaseDao<PlayerAvatar, Long> {

    private final Logger log = LoggerFactory.getLogger(PlayerAvatarDao.class);

    public PlayerAvatarDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerAvatar.class, mongoTemplate);
    }

    /**
     * 获取玩家头像信息
     *
     * @param playerId
     * @return
     */
    public PlayerAvatar getPlayerAvatar(long playerId) {
        return mongoTemplate.findById(playerId, PlayerAvatar.class);
    }


    /**
     * 添加数据
     *
     * @param playerId 玩家id
     * @param ids      配置表ids
     * @return 是否添加成功
     */
    public boolean addByType(long playerId, List<Integer> ids) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        Update update = new Update();
        for (Integer id : ids) {
            AvatarCfg avatarCfg = GameDataManager.getAvatarCfg(id);
            if (avatarCfg == null) {
                log.warn("添加皮肤时未找到配置信息 playerId:{} id:{}", playerId, id);
                continue;
            }
            AvatarType avatarType = EnumUtil.getBy(AvatarType.class, (type -> type.getType() == avatarCfg.getResourceType()));
            if (avatarType == null) {
                log.warn("添加皮肤时未找到对应类型 playerId:{} id:{}", playerId, id);
                continue;
            }
            update.addToSet(avatarType.getField(), id);
        }
        return mongoTemplate.upsert(query, update, PlayerAvatar.class).wasAcknowledged();
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
        return mongoTemplate.upsert(query, update, PlayerAvatar.class).wasAcknowledged();
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
        return mongoTemplate.exists(query, PlayerAvatar.class);
    }


}
