package com.jjg.game.hall.friendroom.dao;

import com.jjg.game.core.dao.room.AbstractFriendRoomDao;
import com.jjg.game.core.data.*;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.springframework.stereotype.Repository;

/**
 * 大厅查询时使用
 *
 * @author 2CL
 */
@Repository
public class HallFriendRoomDao extends AbstractFriendRoomDao<FriendRoom, RoomPlayer> {
    private final String KEY_NAME = ":friendpool:%s";

    public HallFriendRoomDao() {
        super(FriendRoom.class);
    }

    public HallFriendRoomDao(Class<FriendRoom> roomClazz, Class<RoomPlayer> roomPlayerClazz) {
        super(roomClazz);
    }

    @Override
    protected FriendRoom createNewEmptyFriendRoom(WarehouseCfg warehouseCfg) {
        RoomType roomType = RoomType.getRoomType(warehouseCfg.getId());
        switch (roomType) {
            case BET_ROOM, BET_TEAM_UP_ROOM -> {
                return new BetFriendRoom();
            }
            case POKER_ROOM, POKER_TEAM_UP_ROOM -> {
                return new PokerFriendRoom();
            }
            case SLOTS_TEAM_UP_ROOM -> {
                return new SlotsFriendRoom();
            }
        }
        return null;
    }

    @Override
    public long modifyRoomPool(int gameType, long key, long modifyValue) {
        if (modifyValue == 0) {
            return 0;
        }
        Long increment = redisTemplate.opsForValue().increment(getRoomPoolKey(gameType, key), modifyValue);
        return increment == null ? 0 : increment;
    }

    /**
     * 获取 redis房间池数据
     *
     * @param gameType 游戏类型
     * @param roomId   房间 id
     * @return key
     */
    private String getRoomPoolKey(int gameType, long roomId) {
        return getTableName(gameType) + KEY_NAME.formatted(roomId);
    }
}
