package com.jjg.game.core.dao;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author 11
 * @date 2025/6/25 10:22
 */
public abstract class AbstractRoomDao<T extends Room, P extends RoomPlayer> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final String tableName = "room:";
    private final String lockRoomKey = "roomLock:";
    private final String lockNodeCreateKey = "roomNodeCreateLock:";

    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    @Autowired
    protected RedisLock redisLock;
    @Autowired
    PlayerRoomDataDao playerRoomDataDao;

    protected Class<T> roomClazz;
    protected Class<P> roomPlayerClazz;

    public AbstractRoomDao(Class<T> roomClazz, Class<P> roomPlayerClazz) {
        this.roomClazz = roomClazz;
        this.roomPlayerClazz = roomPlayerClazz;
    }

    protected String getTableName(int gameType) {
        return this.tableName + gameType;
    }

    public String getLockName(int gameType, long roomId) {
        return this.lockRoomKey + gameType + ":" + roomId;
    }

    public String getNodeCreateRoomName(int gameType) {
        return this.lockNodeCreateKey + gameType;
    }

    public boolean putIfAbsent(T room) {
        return redisTemplate.opsForHash().putIfAbsent(getTableName(room.getGameType()), room.getId(), room);
    }

    public T nodeCreate(int gameType, int roomCfgId, int maxLimit, String nodeName) {
        try {
            T room = fillBaseRoomData(nodeName, gameType, maxLimit);
            room.setRoomCfgId(roomCfgId);

            String lockKey = getNodeCreateRoomName(gameType);
            for (int i = 0; i < CoreConst.Common.REDIS_TRY_COUNT; i++) {
                if (redisLock.lock(lockKey)) {
                    try {
                        T temp = createRoom(room);
                        if (temp != null) {
                            return temp;
                        }
                    } catch (Exception e) {
                        log.warn("系统创建房间出现异常,gameType = {},", gameType, e);
                    } finally {
                        redisLock.unlock(lockKey);
                    }
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    log.warn("保存房间出现异常,gameType = {}", gameType, e);
                }

            }
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * 创建房间
     */
    public T createRoom(long playerId, int gameType, int maxLimit, String nodeName) {
        try {

            T room = fillBaseRoomData(nodeName, gameType, maxLimit);
            //添加玩家
            if (playerId > 0) {
                RoomPlayer roomPlayer = createRoomPlayer(playerId);
                roomPlayer.setSit(0);
                roomPlayer.setOnline(true);

                Map<Long, RoomPlayer> roomPlayers = new HashMap<>();
                roomPlayers.put(roomPlayer.getPlayerId(), roomPlayer);
                room.setRoomPlayers(roomPlayers);
                room.setCreator(roomPlayer.getPlayerId());

                Map<Integer, Long> playerSits = new HashMap<>();
                playerSits.put(0, roomPlayer.getPlayerId());
                room.setPlayerSits(playerSits);
            }

            return createRoom(room);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    private T fillBaseRoomData(String nodeName, int gameType, int maxLimit) throws InvocationTargetException,
        InstantiationException,
        IllegalAccessException,
        NoSuchMethodException {
        EGameType eGameType = EGameType.getGameByTypeId(gameType);
        Constructor<? extends Room> constructor = eGameType.getRoomType().getRoomDataType().getConstructor();
        T room = (T) constructor.newInstance();
        room.setCreateTime(TimeHelper.nowInt());
        room.setPath(nodeName);
        room.setType(eGameType.getRoomType());
        room.setGameType(gameType);
        room.setMaxLimit(maxLimit);
        return room;
    }

    /**
     * 创建房间
     *
     * @param room
     * @return
     */
    protected T createRoom(T room) {
        try {
            //随机房间号
            // TODO 游戏房间节点处于分布式环境下如何保证房间ID的唯一性,仅在本地100000-999999的范围随机,不能保证不会产生一样的房间ID，并且上层调用并未检查房间ID的合法性
            // FIXME 1.优先考虑使用雪花算法生成ID，如果房间需要展示ID，可以截取ID的后8位
            // FIXME 2.如果需要保证无序的房间ID，可以预先随机填充N个一定范围内的唯一ID到redis的List中，再按序取出
            long roomId = RandomUtils.randomMinMax(GameConstant.Common.ROOM_ID_MIN, GameConstant.Common.ROOM_ID_MAX);
            room.setId(roomId);

            log.debug("创建房间是生成的房间id = {}", roomId);

            boolean success = putIfAbsent(room);
            if (success) {
                return room;
            }
        } catch (Exception e) {
            log.error("创建房间时异常: {}", e.getMessage(), e);
        }
        return null;
    }

    public void saveRoom(T room) {
        redisTemplate.opsForHash().put(getTableName(room.getGameType()), room.getId(), room);
    }

    public CommonResult<? extends Room> doSave(int gameType, long roomId, DataSaveCallback<Room> roomCallback) {
        CommonResult<Room> result = new CommonResult<>(Code.SUCCESS);
        String key = getLockName(gameType, roomId);
        for (int i = 0; i < CoreConst.Common.REDIS_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    T room = getRoom(gameType, roomId);
                    if (roomCallback.updateDataWithRes(room)) {
                        saveRoom(room);
                        result.data = room;
                    } else {
                        result.code = Code.ERROR_REQ;
                    }
                    return result;
                } catch (Exception e) {
                    log.warn("保存房间出现异常,gameType = {},roomId = {}", gameType, roomId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.warn("保存房间出现异常2,gameType = {},roomId = {}", gameType, roomId, e);
            }
        }
        result.code = Code.FAIL;
        return result;
    }

    /**
     * 获取房间数据
     *
     * @param gameType
     * @param roomId
     * @return
     */
    public T getRoom(int gameType, long roomId) {
        return (T) redisTemplate.opsForHash().get(getTableName(gameType), roomId);
    }

    /**
     * 删除房间
     *
     * @param gameType
     * @param roomId
     */
    public Long removeRoom(int gameType, long roomId, int wareId) {
        String key = getLockName(gameType, roomId);
        for (int i = 0; i < CoreConst.Common.REDIS_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    T room = getRoom(gameType, roomId);
                    if (room != null && room.empty()) {
                        return redisTemplate.opsForHash().delete(getTableName(gameType), roomId);
                    }
                    return null;
                } catch (Exception e) {
                    log.warn("清除房间出现异常,gameType = {},roomId = {}", gameType, roomId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.warn("清除房间出现异常,gameType = {},roomId = {}", gameType, roomId, e);
            }
        }
        return null;
    }

    public boolean removePlayer(int gameType, long roomId, long playerId) {
        String key = getLockName(gameType, roomId);
        for (int i = 0; i < CoreConst.Common.REDIS_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    T room = getRoom(gameType, roomId);
                    if (room != null) {
                        room.exit(playerId);
                        saveRoom(room);
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    log.warn("从房间移除玩家数据异常,gameType = {},roomId = {},playerId = {}", gameType, roomId, playerId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.warn("从房间移除玩家数据异常2,gameType = {},roomId = {},playerId = {}", gameType, roomId, playerId, e);
            }
        }
        return false;
    }

    public int getCanJoinRoomId(int gameType, int roomCfgId) {
        return 0;
    }

    /**
     * 已存在的房间个数
     *
     * @param gameType
     * @return
     */
    public long existRoomCount(int gameType, int roomCfgId) {
        return redisTemplate.opsForHash().size(getTableName(gameType));
    }

    public List<Object> getAllRoomIds(int gameType, int roomCfgId) {
        return null;
    }

    public RoomPlayer createRoomPlayer(long playerId) throws Exception {
        //创建roomPlayer对象
        Constructor<? extends RoomPlayer> roomPlayerConstructor = this.roomPlayerClazz.getConstructor();
        RoomPlayer roomPlayer = roomPlayerConstructor.newInstance();
        roomPlayer.setPlayerId(playerId);
        Optional<PlayerRoomData> roomData = playerRoomDataDao.findById(playerId);
        roomPlayer.setPlayerRoomData(roomData.orElse(new PlayerRoomData()));
        return roomPlayer;
    }
}
