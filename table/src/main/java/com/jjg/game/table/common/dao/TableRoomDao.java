package com.jjg.game.table.common.dao;

import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.dao.AbstractGoldRoomDao;
import org.springframework.stereotype.Component;

/**
 * Table房间Dao
 *
 * @author 2CL
 */
@Component
public class TableRoomDao extends AbstractGoldRoomDao<BetTableRoom, RoomPlayer> {
    private final String KEY_NAME = ":pool:%s";

    public TableRoomDao() {
        super(BetTableRoom.class, RoomPlayer.class);
    }

    /**
     * 修改房间奖池
     *
     * @param gameType    游戏类型
     * @param roomCfgId   房间场次类型
     * @param modifyValue 修改的值
     * @return 修改后的值
     */
    @Override
    public long modifyRoomPool(int gameType, long roomCfgId, long modifyValue) {
        if (modifyValue == 0) {
            return 0;
        }
        Long increment = redisTemplate.opsForValue().increment(getRoomPoolKey(gameType, roomCfgId), modifyValue);
        return increment == null ? 0 : increment;
    }

    /**
     * 获取当前奖池
     *
     * @param gameType  游戏类型
     * @param roomCfgId 游戏场次类型
     * @return 当前奖池数量
     */
    public long getRoomPool(int gameType, int roomCfgId, long initRoomPool) {
        Object object = redisTemplate.opsForValue().get(getRoomPoolKey(gameType, roomCfgId));
        switch (object) {
            case null -> {
                Boolean absent = redisTemplate.opsForValue().setIfAbsent(getRoomPoolKey(gameType, roomCfgId), initRoomPool);
                if (Boolean.FALSE.equals(absent)) {
                    log.error("初始化房间池失败，gameType: {}, roomCfgId: {}", gameType, roomCfgId);
                }
                log.info("初始化奖池成功 gameType = {}, roomCfgId = {}, pool = {}", gameType, roomCfgId, initRoomPool);
                return initRoomPool;
            }
            case Integer pool -> {
                return pool;
            }
            case Long poolLong -> {
                return poolLong;
            }
            default -> {
                return 0;
            }
        }
    }

    /**
     * 获取 redis房间池数据
     *
     * @param gameType  游戏类型
     * @param roomCfgId 房间场次类型
     * @return key
     */
    private String getRoomPoolKey(int gameType, long roomCfgId) {
        return getTableName(gameType) + KEY_NAME.formatted(roomCfgId);
    }

}
