package com.jjg.game.table.common.dao;

import com.jjg.game.core.dao.room.AbstractFriendRoomDao;
import com.jjg.game.core.data.BetFriendRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.springframework.stereotype.Repository;

/**
 * 押注类好友房dao
 *
 * @author 2CL
 */
@Repository
public class BetTableFriendRoomDao extends AbstractFriendRoomDao<BetFriendRoom, RoomPlayer> {
    private final String KEY_NAME = ":friendpool:%s";

    public BetTableFriendRoomDao() {
        super(BetFriendRoom.class);
    }

    @Override
    protected BetFriendRoom createNewEmptyFriendRoom(WarehouseCfg warehouseCfg) {
        return new BetFriendRoom();
    }

    /**
     * 修改房间奖池
     *
     * @param gameType    游戏类型
     * @param roomId      房间 Id
     * @param modifyValue 修改的值
     * @return 修改后的值
     */
    @Override
    public long modifyRoomPool(int gameType, long roomId, long modifyValue) {
        if (modifyValue == 0) {
            return 0;
        }
        Long increment = redisTemplate.opsForValue().increment(getRoomPoolKey(gameType, roomId), modifyValue);
        return increment == null ? 0 : increment;
    }

    /**
     * 获取当前奖池
     *
     * @param gameType 游戏类型
     * @param roomId   房间 id
     * @return 当前奖池数量
     */
    public long getRoomPool(int gameType, long roomId) {
        Object object = redisTemplate.opsForValue().get(getRoomPoolKey(gameType, roomId));
        if (object instanceof Integer pool) {
            return pool;
        } else if (object instanceof Long poolLong) {
            return poolLong;
        } else {
            return 0;
        }
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

    /**
     * 设置 redis房间池数据
     *
     * @param gameType 游戏类型
     * @param roomId   房间 id
     * @return key
     */
    public boolean setRoomPoolKey(int gameType, long roomId, long initRoomPool) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(getRoomPoolKey(gameType, roomId), initRoomPool));
    }

    @Override
    public void removeRoomPool(int gameType, long roomId) {
        redisTemplate.delete(getRoomPoolKey(gameType, roomId));
    }
}
