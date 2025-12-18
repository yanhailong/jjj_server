package com.jjg.game.core.dao.room;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.manager.SnowflakeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
    protected NodeManager nodeManager;

    @Lazy
    @Autowired
    protected SnowflakeManager snowflakeManager;

    protected Class<T> roomClazz;

    public AbstractRoomDao(Class<T> roomClazz) {
        this.roomClazz = roomClazz;
    }

    public String getTableName(int gameType) {
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
        boolean lock = false;
        try {
            lock = redisLock.tryLock(lockKey, TimeHelper.ONE_DAY_OF_MILLIS * 3);
            if (!lock) {
                log.error("获取锁失败 lockKey:{} gameType:{} roomCfgId:{} maxLimit:{} nodeName:{}", lockKey, gameType, roomCfgId, maxLimit, nodeName);
                return null;
            }
            T room = fillBaseRoomData(nodeName, gameType, roomCfgId, maxLimit);
            T temp = createRoom(room);
            if (temp != null) {
                return temp;
            }
        } catch (Exception e) {
            log.error("系统创建房间出现异常,gameType = {},", gameType, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(lockKey);
            }
        }
        return null;
    }

    /**
     * 创建房间
     */
    public T createRoom(int gameType, int roomCfgId, int maxLimit, String nodeName) {
        try {
            T room = fillBaseRoomData(nodeName, gameType, roomCfgId, maxLimit);
            return createRoom(room);
        } catch (Exception e) {
            log.error("创建房间异常: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 预创建房间
     */
    public T preCreateRoom(int gameType, int roomCfgId, int maxLimit, String nodeName) {
        try {
            T room = fillBaseRoomData(nodeName, gameType, roomCfgId, maxLimit);
            return createRoom(room, false);
        } catch (Exception e) {
            log.error("预创建房间异常: {}", e.getMessage(), e);
        }
        return null;
    }

    protected T fillBaseRoomData(String nodeName, int gameType, int gameCfgId, int maxLimit) throws InvocationTargetException,
            InstantiationException,
            IllegalAccessException,
            NoSuchMethodException {
        RoomType roomType = RoomType.getRoomType(gameCfgId);
        Constructor<? extends Room> constructor = roomType.getRoomDataType().getConstructor();
        T room = (T) constructor.newInstance();
        room.setCreateTime(TimeHelper.nowInt());
        room.setPath(nodeName);
        room.setType(roomType);
        room.setGameType(gameType);
        room.setMaxLimit(maxLimit);
        room.setRoomCfgId(gameCfgId);
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
                .filter(r -> !(r instanceof FriendRoom))
                .filter(r -> r.getPath().equalsIgnoreCase(currentNodePath) && r.getRoomCfgId() == roomCfgId)
                .toList();
    }

    /**
     * 获取对应当前节点所有的房间
     */
    public List<T> getChooseNodeRoom(String nodePath, int gameType, int roomCfgId) {
        List<Object> rooms = redisTemplate.opsForHash().values(getTableName(gameType));
        return rooms.stream()
                .map(r -> (T) r)
                .filter(r -> !(r instanceof FriendRoom) && r.getPath().equalsIgnoreCase(nodePath)
                        && r.getRoomCfgId() == roomCfgId && r.canEnter())
                .toList();
    }

    /**
     * 创建房间
     */
    protected T createRoom(T room, boolean save) {
        try {
            //随机房间号
            long roomId = snowflakeManager.nextId();
            room.setId(roomId);
            if (!save) {
                return room;
            }
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

    /**
     * 创建房间
     */
    protected T createRoom(T room) {
        return createRoom(room, true);
    }

    // 禁止向外部暴露可以直接操作房间的接口
    protected void saveRoom(T room) {
        redisTemplate.opsForHash().put(getTableName(room.getGameType()), room.getId(), room);
    }

    /**
     * 保存方法
     */
    public CommonResult<T> doSave(T room, DataSaveCallback<T> roomCallback) {
        return doSave(room.getGameType(), room.getId(), roomCallback);
    }


    /**
     * 保存方法
     */
    public CommonResult<T> doSave(int gameType, long roomId, DataSaveCallback<T> roomCallback) {
        CommonResult<T> result = new CommonResult<>(Code.SUCCESS);
        String key = getLockName(gameType, roomId);
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                result.code = Code.FAIL;
                log.error("获取锁失败 lockKey:{} gameType:{} roomId:{}", key, gameType, roomId);
                return result;
            }
            T room = getRoom(gameType, roomId);
            boolean updateDataWithRes = roomCallback.updateDataWithRes(room);
            if (updateDataWithRes) {
                saveRoom(room);
                result.data = room;
            } else {
                result.code = Code.ERROR_REQ;
            }
            return result;
        } catch (Exception e) {
            log.warn("保存房间出现异常,gameType = {},roomId = {}", gameType, roomId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(key);
            }
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
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.error("获取锁失败 lockKey:{} gameType:{} roomId:{} wareId:{}", key, gameType, roomId, wareId);
                return null;
            }
            T room = getRoom(gameType, roomId);
            if (room != null) {
                return redisTemplate.opsForHash().delete(getTableName(gameType), roomId);
            }
            return null;
        } catch (Exception e) {
            log.warn("清除房间出现异常,gameType = {},roomId = {}", gameType, roomId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(key);
            }
        }
        return null;
    }

    public T removePlayer(int gameType, long roomId, long playerId) {
        String key = getLockName(gameType, roomId);
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.error("获取锁失败 lockKey:{} gameType:{} roomId:{} playerId:{}", key, gameType, roomId, playerId);
                return null;
            }
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
            if (lock) {
                redisLock.tryUnlock(key);
            }
        }
        return null;
    }

    /**
     * 更新房间玩家信息
     *
     * @param gameType       游戏类型
     * @param roomId         房间id
     * @param playerId       玩家id
     * @param updateFunction 更新函数
     * @return 更新后的房间数据
     */
    public T updateRoomPlayer(int gameType, long roomId, long playerId, Consumer<RoomPlayer> updateFunction) {
        String key = getLockName(gameType, roomId);
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.error("获取锁失败 lockKey:{} gameType:{} roomId:{} playerId:{}", key, gameType, roomId, playerId);
                return null;
            }
            T room = getRoom(gameType, roomId);
            if (room != null) {
                Map<Long, RoomPlayer> roomPlayers = room.getRoomPlayers();
                if (CollectionUtil.isEmpty(roomPlayers)) {
                    return null;
                }
                RoomPlayer roomPlayer = roomPlayers.get(playerId);
                if (roomPlayer == null) {
                    return null;
                }
                updateFunction.accept(roomPlayer);
                saveRoom(room);
                return room;
            }
            return null;
        } catch (Exception e) {
            log.warn("房间更新玩家数据异常,gameType = {},roomId = {},playerId = {}", gameType, roomId, playerId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(key);
            }
        }
        return null;
    }

    /**
     * 更新房间内的座位信息
     *
     * @param gameType       房间类型
     * @param roomId         房间id
     * @param playerId       玩家id
     * @param newSitIndex    新座位id
     * @param forcedExchange 是否强制交换
     * @return 更新后的房间信息
     */
    public T updateRoomPlayerSitInfo(int gameType, long roomId, long playerId, int newSitIndex, boolean forcedExchange) {
        String key = getLockName(gameType, roomId);
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.error("获取锁失败 lockKey:{} gameType:{} roomId:{} playerId:{} newSitIndex:{}", key, gameType, roomId, playerId, newSitIndex);
                return null;
            }
            T room = getRoom(gameType, roomId);
            if (room != null) {
                Map<Long, RoomPlayer> roomPlayers = room.getRoomPlayers();
                if (CollectionUtil.isEmpty(roomPlayers)) {
                    return null;
                }
                RoomPlayer roomPlayer = roomPlayers.get(playerId);
                if (roomPlayer == null) {
                    return null;
                }
                Map<Integer, Long> playerSits = room.getPlayerSits();
                if (CollectionUtil.isEmpty(playerSits)) {
                    return null;
                }
                Long oldPlayerId = playerSits.get(roomPlayer.getSit());
                if (!oldPlayerId.equals(playerId)) {
                    log.error("player:{} 更新玩家座位时 老座位不是对应玩家", playerId);
                    return null;
                }
                Long newSitId = playerSits.getOrDefault(newSitIndex, -1L);
                if (newSitId != null && !forcedExchange) {
                    log.error("player:{} 更新玩家座位时 新座位有玩家", playerId);
                    return null;
                }
                //交换
                RoomPlayer roomPlayerNew = roomPlayers.get(newSitId);
                if (roomPlayerNew == null) {
                    playerSits.remove(roomPlayer.getSit());
                    roomPlayer.setSit(newSitIndex);
                    playerSits.put(newSitIndex, roomPlayer.getPlayerId());
                } else {
                    //交换座位
                    //设置目标的座位
                    roomPlayerNew.setSit(roomPlayer.getSit());
                    playerSits.put(roomPlayerNew.getSit(), roomPlayerNew.getPlayerId());
                    //设置要交换的座位
                    roomPlayer.setSit(newSitIndex);
                    playerSits.put(roomPlayer.getSit(), roomPlayer.getPlayerId());
                }
                saveRoom(room);
                return room;
            }
            return null;
        } catch (Exception e) {
            log.warn("房间更新玩家数据异常,gameType = {},roomId = {},playerId = {}", gameType, roomId, playerId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(key);
            }
        }
        return null;
    }

    /**
     * 批量退出玩家
     */
    public T removePlayers(int gameType, long roomId, List<Long> playerIds) {
        String key = getLockName(gameType, roomId);
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.error("获取锁失败 lockKey:{} roomId:{} playerIds:{} ", key, roomId, playerIds);
                return null;
            }
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
            if (lock) {
                redisLock.tryUnlock(key);
            }
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

    public RoomPlayer createRoomPlayer(PlayerController playerController) {
        long playerId = playerController.playerId();
        //创建roomPlayer对象
        RoomPlayer roomPlayer = new RoomPlayer();
        roomPlayer.setPlayerId(playerId);
        return roomPlayer;
    }

    /**
     * 删除房间奖池
     *
     * @param gameType 游戏类型
     * @param key   key
     */
    public void removeRoomPool(int gameType, long key) {
    }

    /**
     * 修改房间奖池
     *
     * @param gameType    游戏类型
     * @param key         key
     * @param modifyValue 修改的值
     * @return 修改后的值
     */
    public long modifyRoomPool(int gameType, long key, long modifyValue) {
        return 0;
    }
}
