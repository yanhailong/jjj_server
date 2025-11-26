package com.jjg.game.core.dao.room;

import com.jjg.game.common.constant.StrConstant;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.springframework.data.redis.core.RedisCallback;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 好友房Dao
 *
 * @author 2CL
 */
public abstract class AbstractFriendRoomDao<T extends FriendRoom, P extends RoomPlayer> extends AbstractRoomDao<T, P> {

    // 玩家好友房
    private static final String PLAYER_FRIEND_ROOM = "PlayerFriendRoomId";

    public AbstractFriendRoomDao(Class<T> roomClazz) {
        super(roomClazz);
    }

    private String getPlayerFriendRoomTableName(long playerId) {
        // 好友房表名 Hash PlayerFriendRoomId+玩家ID 房间ID <=> 时间
        String playerFriendRoom = PLAYER_FRIEND_ROOM + StrConstant.COLON;
        return playerFriendRoom + playerId;
    }

    public static class CreateFriendsRoom {
        // 请求使用的道具ID 金币和券ID
        public int itemId;
        // 房间配置ID，场次ID
        public int roomCfgId;
        // 申请开房时长
        public int timeOfOpenRoom;
        // 是否自动续费
        public boolean autoRenewal;
        // 庄家准备金
        public long predictCostGoldNum;
        // 房间名
        public String roomAliasName;
        // 房间ID
        public int roomExpandId;

        public CreateFriendsRoom(int itemId, int roomCfgId, int timeOfOpenRoom, boolean autoRenewal,
                                 long predictCostGoldNum, String roomAliasName, int roomExpandId) {
            this.itemId = itemId;
            this.roomCfgId = roomCfgId;
            this.timeOfOpenRoom = timeOfOpenRoom;
            this.autoRenewal = autoRenewal;
            this.predictCostGoldNum = predictCostGoldNum;
            this.roomAliasName = roomAliasName;
            this.roomExpandId = roomExpandId;
        }
    }

    /**
     * 创建押注好友房
     */
    public FriendRoom createBetFriendRoom(
            long playerId, String nodePath, WarehouseCfg warehouseCfg, CreateFriendsRoom req) {
        try {
            int gameType = warehouseCfg.getGameID();
            int roomCfgId = warehouseCfg.getId();
            Tuple2<Integer, Integer> roomMaxLimit = SampleDataUtils.getRoomMaxLimit(warehouseCfg);
            int maxLimit = roomMaxLimit.getT1();
            long curTime = System.currentTimeMillis();
            T friendRoom = fillFriendRoomData(gameType, warehouseCfg, nodePath, maxLimit);
            friendRoom.setRoomCfgId(roomCfgId);
            friendRoom.setAliasName(req.roomAliasName);
            friendRoom.setOverdueTime(req.timeOfOpenRoom + curTime);
            // 如果时间大于0需要立马开始计时
            if (req.timeOfOpenRoom > 0) {
                friendRoom.setStatus(1);
            }
            friendRoom.setAutoRenewal(req.autoRenewal);
            friendRoom.setPredictCostGoldNum(req.predictCostGoldNum);
            friendRoom.setCreator(playerId);
            friendRoom.setRoomExpendId(req.roomExpandId);
            FriendRoom savedRoom = createRoom(friendRoom);
            String tableName = getPlayerFriendRoomTableName(playerId);
            redisTemplate.opsForHash().put(tableName, savedRoom.getId(), gameType);
            return savedRoom;
        } catch (Exception e) {
            log.error("创建好友房出现异常, {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
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
                String tableName = getPlayerFriendRoomTableName(room.getCreator());
                redisTemplate.opsForHash().delete(tableName, roomId);
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

    /**
     * 获取玩家好友房数量
     */
    public Map<Long, Integer> getPlayerFriendRoomNum(List<Long> playerIds) {
        List<Integer> playerRoomNumList = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (long playerId : playerIds) {
                        String tableName = getPlayerFriendRoomTableName(playerId);
                        connection.hashCommands().hLen(tableName.getBytes());
                    }
                    return null;
                }).stream()
                .map(a -> ((Long) a).intValue())
                .toList();
        Map<Long, Integer> playerRoomNumMap = new HashMap<>();
        for (int i = 0; i < playerIds.size(); i++) {
            playerRoomNumMap.put(playerIds.get(i), playerRoomNumList.get(i));
        }
        return playerRoomNumMap;
    }

    /**
     * 填充好友房数据
     */
    protected T fillFriendRoomData(int gameType, WarehouseCfg warehouseCfg, String nodeName, int maxLimit) {
        RoomType roomType = RoomType.getRoomType(warehouseCfg.getId());
        T room = createNewEmptyFriendRoom(warehouseCfg);
        if (room == null) {
            throw new IllegalArgumentException("通过房间类型：" + gameType + " 创建好友房异常,找不到游戏类型对应的房间类型");
        }
        room.setCreateTime(TimeHelper.nowInt());
        room.setPath(nodeName);
        room.setType(roomType);
        room.setGameType(gameType);
        room.setMaxLimit(maxLimit);
        return room;
    }

    protected abstract T createNewEmptyFriendRoom(WarehouseCfg warehouseCfg);

    /**
     * 获取玩家房间数量
     */
    public long getPlayerRoomSize(long playerId) {
        String playerFriendRoomTableName = getPlayerFriendRoomTableName(playerId);
        return redisTemplate.opsForHash().size(playerFriendRoomTableName);
    }

    /**
     * 获取好友房
     */
    public FriendRoom getFriendRoomById(long playerId, long roomId) {
        String playerFriendRoomTableName = getPlayerFriendRoomTableName(playerId);
        Object gameType = redisTemplate.opsForHash().get(playerFriendRoomTableName, roomId);
        if (gameType == null) {
            return null;
        }
        return getRoom((int) gameType, roomId);
    }

    /**
     * 获取玩家所有的好友房
     */
    public List<FriendRoom> getPlayerAllFriendRoom(long playerId) {
        String playerFriendRoomTableName = getPlayerFriendRoomTableName(playerId);
        Map<Object, Object> roomIds = redisTemplate.opsForHash().entries(playerFriendRoomTableName);
        // 游戏类型，房间ID列表
        Map<Object, List<Object>> gameOfIdList =
                roomIds.entrySet().stream()
                        .collect(HashMap::new, (map, e) -> {
                            map.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
                        }, HashMap::putAll);
        // 批量获取房间
        List<Object> roomObjectList = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Map.Entry<Object, List<Object>> entry : gameOfIdList.entrySet()) {
                byte[][] roomIdByteArr = new byte[entry.getValue().size()][];
                Object[] roomIdArr = entry.getValue().toArray(new Object[0]);
                for (int i = 0; i < roomIdArr.length; i++) {
                    roomIdByteArr[i] = (roomIdArr[i] + "").getBytes();
                }
                String tableName = getTableName((int) entry.getKey());
                connection.hashCommands().hMGet(tableName.getBytes(), roomIdByteArr);
            }
            return null;
        });
        if (roomObjectList.isEmpty()) {
            return new ArrayList<>();
        }
        return roomObjectList.stream()
                .map(a -> a == null ? null : (List) a)
                .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll)
                .stream()
                .filter(Objects::nonNull)
                .map((a) -> (FriendRoom) a)
                .collect(Collectors.toList());
    }
}
