package com.jjg.game.core.dao.room;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.core.data.PlayerRoomData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * 玩家房间数据DAO
 *
 * @author 2CL
 */
@Repository
public class PlayerRoomDataDao extends MongoBaseDao<PlayerRoomData, Long> {

    public PlayerRoomDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerRoomData.class, mongoTemplate);
    }
}
