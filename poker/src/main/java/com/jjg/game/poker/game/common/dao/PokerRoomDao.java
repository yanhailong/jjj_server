package com.jjg.game.poker.game.common.dao;

import com.jjg.game.core.data.PokerRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.dao.AbstractGoldRoomDao;
import org.springframework.stereotype.Component;

/**
 * Table房间Dao
 *
 * @author 2CL
 */
@Component
public class PokerRoomDao extends AbstractGoldRoomDao<PokerRoom, RoomPlayer> {
    private final String KEY_NAME = "pool:%s";

    public PokerRoomDao() {
        super(PokerRoom.class);
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
        return redisTemplate.opsForHash().increment(getRoomPoolKey(gameType), roomCfgId, modifyValue);
    }

    /**
     * 获取当前奖池
     *
     * @param gameType  游戏类型
     * @param roomCfgId 游戏场次类型
     * @return 当前奖池数量
     */
    public long getRoomPool(int gameType, int roomCfgId, long initRoomPool) {
        Object object = redisTemplate.opsForHash().get(getRoomPoolKey(gameType), roomCfgId);
        switch (object) {
            case null -> {
                Boolean absent = redisTemplate.opsForHash().putIfAbsent(getRoomPoolKey(gameType), roomCfgId, initRoomPool);
                if (!absent) {
                    log.error("初始化房间池失败，gameType: {}, roomCfgId: {}", gameType, roomCfgId);
                    return getRoomPool(gameType, roomCfgId, initRoomPool);
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
     * @param gameType 游戏类型
     * @return key
     */
    private String getRoomPoolKey(int gameType) {
        return KEY_NAME.formatted(gameType);
    }
}
