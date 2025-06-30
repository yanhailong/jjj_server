package com.jjg.game.room.dao;

import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/25 18:15
 */
public abstract class AbstractGoldRoomDao<T extends Room,P extends RoomPlayer> extends AbstractRoomDao<T,P> {

    private final String roomIdListKey = "roomIdList:";

    public AbstractGoldRoomDao(Class<T> roomClazz,Class<P> roomPlayerClazz) {
        super(roomClazz,roomPlayerClazz);
    }

    protected String getRoomIdListKey(int gameType,int wareId) {
        return roomIdListKey + gameType + ":" + wareId;
    }

    @Override
    public <T extends Room> boolean putIfAbsent(T room) {
        boolean success = super.putIfAbsent(room);
        if(success){
            redisTemplate.opsForList().rightPush(getRoomIdListKey(room.getGameType(),room.getWareId()), room.getId());
        }
        return success;
    }

    @Override
    public Long removeRoom(int gameType, int roomId, int wareId) {
        long res = super.removeRoom(gameType, roomId, wareId);
        if(res > 0){
            //从roomid列表中移除，-1表示从尾部移除1个
            redisTemplate.opsForList().remove(getRoomIdListKey(gameType,wareId), -1, roomId);
        }
        return res;
    }

    /**
     * 从房间id列表中获取一个房间id
     * @return
     */
    @Override
    public int getCanJoinRoomId(int gameType, int wareId) {
        //返回list头部元素
        Object o =  redisTemplate.opsForList().index(getRoomIdListKey(gameType,wareId), 0);
        if(o == null){
            return 0;
        }
        return Integer.parseInt(o.toString());
    }

    @Override
    public long existRoomCount(int gameType, int wareId) {
        return redisTemplate.opsForList().size(getRoomIdListKey(gameType,wareId));
    }

    @Override
    public List<Object> getAllRoomIds(int gameType, int wareId) {
        return redisTemplate.opsForList().range(getRoomIdListKey(gameType,wareId), 0, -1);
    }
}
