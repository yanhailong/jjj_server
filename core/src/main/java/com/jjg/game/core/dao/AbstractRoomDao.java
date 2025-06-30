package com.jjg.game.core.dao;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.RoomType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/25 10:22
 */
public abstract class AbstractRoomDao<T extends Room,P extends RoomPlayer> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final String tableName = "room:";
    private final String lockRoomKey = "roomLock:";
    private final String lockNodeCreateKey = "roomNodeCreateLock:";

    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    @Autowired
    protected RedisLock redisLock;

    protected Class<T> roomClazz;
    protected Class<P> roomPlayerClazz;

    public AbstractRoomDao(Class<T> roomClazz,Class<P> roomPlayerClazz) {
        this.roomClazz = roomClazz;
        this.roomPlayerClazz = roomPlayerClazz;
    }

    public interface RoomCallback {
        boolean exe(Room room);
    }

    protected String getTableName(int gameType) {
        return this.tableName + gameType;
    }

    public String getLockName(int gameType, int roomId) {
        return this.lockRoomKey + gameType + ":" + roomId;
    }

    public String getNodeCreateRoomName(int gameType) {
        return this.lockNodeCreateKey + gameType;
    }

    public <T extends Room> boolean putIfAbsent(T room) {
        return redisTemplate.opsForHash().putIfAbsent(getTableName(room.getGameType()), room.getId(), room);
    }

    public Room nodeCreate(int gameType,int wareId,int maxLimit,String nodeName,RoomType roomType) {
        try{
            // 获取构造函数
            Constructor<? extends Room> constructor = this.roomClazz.getConstructor();

            Room room = constructor.newInstance();
            room.setCreateTime(TimeHelper.nowInt());
            room.setPath(nodeName);
            room.setType(roomType);
            room.setGameType(gameType);
            room.setWareId(wareId);
            room.setMaxLimit(maxLimit);

            String lockKey = getNodeCreateRoomName(gameType);
            for(int i=0;i< CoreConst.Common.REDIS_TRY_COUNT;i++){
                if (redisLock.lock(lockKey)) {
                    try {
                        Room temp = createRoom(room);
                        if(temp != null){
                            return temp;
                        }
                    } catch (Exception e) {
                        log.warn("系统创建房间出现异常,gameTpye = {},",gameType, e);
                    } finally {
                        redisLock.unlock(lockKey);
                    }
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    log.warn("保存房间出现异常,gameTpye = {}",gameType, e);
                }

            }
        }catch (Exception e) {
            log.error("",e);
        }
        return null;
    }

    public Room createRoom(long playerId,int gameType,int maxLimit,String nodeName,RoomType roomType) {
        try{
            // 获取构造函数
            Constructor<? extends Room> constructor = this.roomClazz.getConstructor();

            Room room = constructor.newInstance();
            room.setCreateTime(TimeHelper.nowInt());
            room.setPath(nodeName);
            room.setType(roomType);
            room.setGameType(gameType);
            room.setMaxLimit(maxLimit);

            //添加玩家
            if(playerId > 0){
                RoomPlayer roomPlayer = createRoomPlayer(playerId);
                roomPlayer.setSit(0);
                roomPlayer.setOnline(true);

                Map<Long,RoomPlayer> roomPlayers = new HashMap<>();
                roomPlayers.put(roomPlayer.getPlayerId(), roomPlayer);
                room.setRoomPlayers(roomPlayers);
                room.setCreator(roomPlayer.getPlayerId());

                Map<Integer,Long> playerSits = new HashMap<>();
                playerSits.put(0, roomPlayer.getPlayerId());
                room.setPlayerSits(playerSits);
            }

            return createRoom(room);
        }catch (Exception e) {
            log.error("",e);
        }
        return null;
    }

    /**
     * 创建房间
     * @param room
     * @return
     */
    protected Room createRoom(Room room) {
        try {
            //随机房间号
            int roomId = RandomUtils.randomMinMax(GameConstant.Common.ROOM_ID_MIN,GameConstant.Common.ROOM_ID_MAX);
            room.setId(roomId);

            log.debug("创建房间是生成的房间id = {}", roomId);

            boolean success = putIfAbsent(room);
            if (success) {
                return room;
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    public <T extends Room> void saveRoom(T room) {
        redisTemplate.opsForHash().put(getTableName(room.getGameType()), room.getId(), room);
    }

    public CommonResult<Room> doSave(int gameType, int roomId, RoomCallback roomCallback) {
        CommonResult<Room> result = new CommonResult<>(Code.SUCCESS);
        String key = getLockName(gameType, roomId);
        for (int i = 0; i < CoreConst.Common.REDIS_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    Room room = getRoom(gameType,roomId);
                    if (roomCallback.exe(room)) {
                        saveRoom(room);
                        result.data = room;
                    }else {
                        result.code = Code.ERROR_REQ;
                    }
                    return result;
                } catch (Exception e) {
                    log.warn("保存房间出现异常,gameTpye = {},roomId = {}",gameType,roomId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.warn("保存房间出现异常2,gameTpye = {},roomId = {}",gameType,roomId, e);
            }
        }
        result.code = Code.FAIL;
        return result;
    }

    /**
     * 获取房间数据
     * @param gameType
     * @param roomId
     * @return
     */
    public T getRoom(int gameType,int roomId) {
        return (T)redisTemplate.opsForHash().get(getTableName(gameType), roomId);
    }

    /**
     * 删除房间
     * @param gameType
     * @param roomId
     */
    public Long removeRoom(int gameType,int roomId,int wareId) {
        String key = getLockName(gameType, roomId);
        for (int i = 0; i < CoreConst.Common.REDIS_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    T room = getRoom(gameType, roomId);
                    if(room != null && room.empty()){
                        return redisTemplate.opsForHash().delete(getTableName(gameType), roomId);
                    }
                    return null;
                } catch (Exception e) {
                    log.warn("清除房间出现异常,gameTpye = {},roomId = {}",gameType,roomId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.warn("清除房间出现异常,gameTpye = {},roomId = {}",gameType,roomId, e);
            }
        }
        return null;
    }

    public boolean removePlayer(int gameType,int roomId,long playerId) {
        String key = getLockName(gameType, roomId);
        for (int i = 0; i < CoreConst.Common.REDIS_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    T room = getRoom(gameType, roomId);
                    if(room != null){
                        room.exit(playerId);
                        saveRoom(room);
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    log.warn("从房间移除玩家数据异常,gameTpye = {},roomId = {},playerId = {}",gameType,roomId,playerId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.warn("从房间移除玩家数据异常2,gameTpye = {},roomId = {},playerId = {}",gameType,roomId,playerId, e);
            }
        }
        return false;
    }

    public int getCanJoinRoomId(int gameType, int wareId){
        return 0;
    }

    /**
     * 已存在的房间个数
     * @param gameType
     * @return
     */
    public long existRoomCount(int gameType, int wareId){
        return redisTemplate.opsForHash().size(getTableName(gameType));
    }

    public List<Object> getAllRoomIds(int gameType, int wareId){
        return null;
    }

    public RoomPlayer createRoomPlayer(long playerId) throws Exception{
        //创建roomPlayer对象
        Constructor<? extends RoomPlayer> roomPlayerConstructor = this.roomPlayerClazz.getConstructor();
        RoomPlayer roomPlayer = roomPlayerConstructor.newInstance();
        roomPlayer.setPlayerId(playerId);
        return roomPlayer;
    }
}
