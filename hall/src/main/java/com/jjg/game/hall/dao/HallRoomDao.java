package com.jjg.game.hall.dao;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.StrConstant;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.*;
import com.jjg.game.hall.friendroom.message.req.ReqCreateFriendsRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/25 17:05
 */
@Repository
public class HallRoomDao extends AbstractRoomDao<Room, RoomPlayer> {

    public HallRoomDao() {
        super(Room.class, RoomPlayer.class);
    }
}
