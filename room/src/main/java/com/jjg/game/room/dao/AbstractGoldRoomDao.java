package com.jjg.game.room.dao;

import com.jjg.game.core.dao.room.AbstractRoomDao;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;

/**
 * @author 11
 * @date 2025/6/25 18:15
 */
public abstract class AbstractGoldRoomDao<T extends Room,P extends RoomPlayer> extends AbstractRoomDao<T,P> {

    private final String roomIdListKey = "roomIdList:";

    public AbstractGoldRoomDao(Class<T> roomClazz) {
        super(roomClazz);
    }

    protected String getRoomIdListKey(int gameType,int roomCfgId) {
        return roomIdListKey + gameType + ":" + roomCfgId;
    }

    @Override
    public Long removeRoom(int gameType, long roomId, int roomCfgId) {
        Long res = super.removeRoom(gameType, roomId, roomCfgId);
        if(res != null && res > 0){
            //从room id列表中移除，-1表示从尾部移除1个
            redisTemplate.opsForList().remove(getRoomIdListKey(gameType,roomCfgId), -1, roomId);
        }
        return res;
    }

}
