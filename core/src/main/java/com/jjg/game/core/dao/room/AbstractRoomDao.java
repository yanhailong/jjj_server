package com.jjg.game.core.dao.room;

import cn.hutool.core.lang.Snowflake;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.redis.RedisLock;
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
import java.util.stream.Collectors;

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
    private PlayerRoomDataDao playerRoomDataDao;
    @Autowired
    protected NodeManager nodeManager;
    private final Snowflake snowflake = new Snowflake(NodeType.GAME.getValue(), 1);

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

    // 禁止向外部暴露可以直接操作房间的接口
    protected boolean putIfAbsent(T room) {
        return redisTemplate.opsForHash().putIfAbsent(getTableName(room.getGameType()), room.getId(), room);
    }

    public T nodeCreate(int gameType, int roomCfgId, int maxLimit, String nodeName) {
        String lockKey = getNodeCreateRoomName(gameType);
        // 最大等待3分钟
        redisLock.lock(lockKey, TimeHelper.ONE_DAY_OF_MILLIS * 3);
        try {
            T room = fillBaseRoomData(nodeName, gameType, maxLimit);
            room.setRoomCfgId(roomCfgId);
            T temp = createRoom(room);
            if (temp != null) {
                return temp;
            }
        } catch (Exception e) {
            log.error("系统创建房间出现异常,gameType = {},", gameType, e);
        } finally {
            redisLock.unlock(lockKey);
        }
        return null;
    }

    /**
     * 创建房间
     */
    public T createRoom(PlayerController playerController, int gameType, int roomCfgId, int maxLimit, String nodeName) {
        try {
            long playerId = playerController.playerId();
            T room = fillBaseRoomData(nodeName, gameType, maxLimit);
            room.setRoomCfgId(roomCfgId);
            //添加玩家
            if (playerId > 0) {
                RoomPlayer roomPlayer = createRoomPlayer(playerController);
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
            log.error("创建房间异常: {}", e.getMessage(), e);
        }
        return null;
    }

    protected T fillBaseRoomData(String nodeName, int gameType, int maxLimit) throws InvocationTargetException,
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
     * 获取当前节点所有的房间
     */
    public List<T> getCurrentNodeRoom(int gameType, int roomCfgId) {
        String currentNodePath = nodeManager.getNodePath();
        List<Object> rooms = redisTemplate.opsForHash().values(getTableName(gameType));
        return rooms.stream()
            .map(r -> (T) r)
            .filter(r -> r.getPath().equalsIgnoreCase(currentNodePath) && r.getRoomCfgId() == roomCfgId)
            .toList();
    }

    /**
     * 创建房间
     */
    protected T createRoom(T room) {
        try {
            //随机房间号
            long roomId = snowflake.nextId();
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

    // 禁止向外部暴露可以直接操作房间的接口
    protected void saveRoom(T room) {
        redisTemplate.opsForHash().put(getTableName(room.getGameType()), room.getId(), room);
    }

    public CommonResult<? extends Room> doSave(int gameType, long roomId, DataSaveCallback<T> roomCallback) {
        CommonResult<Room> result = new CommonResult<>(Code.SUCCESS);
        String key = getLockName(gameType, roomId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            T room = getRoom(gameType, roomId);
            Boolean updateDataWithRes = roomCallback.updateDataWithRes(room);
            if (updateDataWithRes != null && updateDataWithRes) {
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
        result.code = Code.FAIL;
        return result;
    }

    /**
     * 获取房间数据
     */
    public T getRoom(int gameType, long roomId) {
        return (T) redisTemplate.opsForHash().get(getTableName(gameType), roomId);
    }

    /**
     * 删除房间
     */
    public Long removeRoom(int gameType, long roomId, int wareId) {
        String key = getLockName(gameType, roomId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            T room = getRoom(gameType, roomId);
            if (room != null) {
                return redisTemplate.opsForHash().delete(getTableName(gameType), roomId);
            }
            return null;
        } catch (Exception e) {
            log.warn("清除房间出现异常,gameType = {},roomId = {}", gameType, roomId, e);
        } finally {
            redisLock.unlock(key);
        }
        return null;
    }

    public T removePlayer(int gameType, long roomId, long playerId) {
        String key = getLockName(gameType, roomId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            T room = getRoom(gameType, roomId);
            if (room != null) {
                room.exit(playerId);
                saveRoom(room);
                return room;
            }
            return null;
        } catch (Exception e) {
            log.warn("从房间移除玩家数据异常,gameType = {},roomId = {},playerId = {}", gameType, roomId, playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return null;
    }

    /**
     * 批量退出玩家
     */
    public T removePlayers(int gameType, long roomId, List<Long> playerIds) {
        String key = getLockName(gameType, roomId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            T room = getRoom(gameType, roomId);
            if (room != null) {
                room.exitPlayers(playerIds);
                saveRoom(room);
                return room;
            }
            return null;
        } catch (Exception e) {
            log.warn("从房间移除玩家数据异常,gameType = {},roomId = {},playerId = {}",
                gameType, roomId, playerIds.stream().map(String::valueOf).collect(Collectors.joining(",")), e);
        } finally {
            redisLock.unlock(key);
        }
        return null;
    }

    public int getCanJoinRoomId(int gameType, int roomCfgId) {
        return 0;
    }

    /**
     * 已存在的房间个数
     */
    public long existRoomCount(int gameType, int roomCfgId) {
        return redisTemplate.opsForHash().size(getTableName(gameType));
    }

    public RoomPlayer createRoomPlayer(PlayerController playerController) throws Exception {
        long playerId = playerController.playerId();
        //创建roomPlayer对象
        Constructor<? extends RoomPlayer> roomPlayerConstructor = this.roomPlayerClazz.getConstructor();
        RoomPlayer roomPlayer = roomPlayerConstructor.newInstance();
        roomPlayer.setPlayerId(playerId);
        if (!(playerController.getPlayer() instanceof RobotPlayer)) {
            Optional<PlayerRoomData> roomData = playerRoomDataDao.findById(playerId);
            roomPlayer.setPlayerRoomData(roomData.orElse(new PlayerRoomData()));
            roomPlayer.getPlayerRoomData().setPlayerId(playerId);
        }
        return roomPlayer;
    }
}
