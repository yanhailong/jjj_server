package com.jjg.game.hall.dao;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.StrConstant;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.*;
import com.jjg.game.hall.friendroom.message.req.ReqCreateFriendsRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/25 17:05
 */
@Repository
public class HallRoomDao extends AbstractRoomDao<Room, RoomPlayer> {

    @Autowired
    private NodeConfig nodeConfig;

    public HallRoomDao() {
        super(Room.class, RoomPlayer.class);
    }

    private String getPlayerFriendRoomTableName(long playerId) {
        // 好友房表名 Hash PlayerFriendRoomId+玩家ID 房间ID <=> 时间
        String playerFriendRoom = "PlayerFriendRoomId" + StrConstant.COLON;
        return playerFriendRoom + playerId;
    }

    /**
     * 创建押注好友房
     */
    public FriendRoom createBetFriendRoom(
        long playerId, int gameType, int roomCfgId, int maxLimit, ReqCreateFriendsRoom req) {
        try {
            String nodePath = nodeConfig.getName();
            long curTime = System.currentTimeMillis();
            FriendRoom friendRoom = fillFriendRoomData(gameType, nodePath, maxLimit);
            friendRoom.setRoomCfgId(roomCfgId);
            friendRoom.setAliasName(req.roomAliasName);
            long timeOfOpenRoom = (long) req.timeOfOpenRoom * TimeHelper.ONE_MINUTE_OF_MILLIS;
            friendRoom.setOverdueTime(timeOfOpenRoom + curTime);
            friendRoom.setAutoRenewal(req.autoRenewal);
            friendRoom.setPredictCostGoldNum(req.predictCostGoldNum);
            friendRoom.setCreator(playerId);
            String tableName = getPlayerFriendRoomTableName(playerId);
            redisTemplate.opsForHash().put(tableName, friendRoom.getId(), gameType);
            return (FriendRoom) createRoom(friendRoom);
        } catch (Exception e) {
            log.error("创建好友房出现异常");
        }
        return null;
    }

    /**
     * 填充好友房数据
     */
    protected FriendRoom fillFriendRoomData(int gameType, String nodeName, int maxLimit) {
        EGameType eGameType = EGameType.getGameByTypeId(gameType);
        FriendRoom room = null;
        if (eGameType.getRoomType() == RoomType.BET_ROOM) {
            room = new BetFriendRoom();
        } else if (eGameType.getRoomType() == RoomType.POKER_ROOM) {
            room = new PokerFriendRoom();
        }
        if (room == null) {
            throw new IllegalArgumentException("通过房间类型：" + gameType + " 创建好友房异常,找不到游戏类型对应的房间类型");
        }
        room.setCreateTime(TimeHelper.nowInt());
        room.setPath(nodeName);
        room.setType(eGameType.getRoomType());
        room.setGameType(gameType);
        room.setMaxLimit(maxLimit);
        return room;
    }

    /**
     * 获取玩家房间数量
     */
    public long getPlayerRoomSize(long playerId) {
        String playerFriendRoomTableName = getPlayerFriendRoomTableName(playerId);
        return redisTemplate.opsForHash().size(playerFriendRoomTableName);
    }

    /**
     * 获取玩家所有的好友房
     */
    public List<FriendRoom> getPlayerAllFriendRoom(long playerId) {
        String playerFriendRoomTableName = getPlayerFriendRoomTableName(playerId);
        Map<Object, Object> roomIds = redisTemplate.opsForHash().entries(playerFriendRoomTableName);
        // 游戏类型，放假ID列表
        Map<Object, List<Object>> gameOfIdList =
            roomIds.entrySet().stream()
                .collect(HashMap::new, (map, e) -> {
                    map.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
                }, HashMap::putAll);
        // 批量获取房间
        List<Object> roomObjectList = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Map.Entry<Object, List<Object>> entry : gameOfIdList.entrySet()) {
                byte[][] roomIdByteArr = new byte[entry.getValue().size()][];
                Long[] roomIdArr = entry.getValue().toArray(new Long[0]);
                for (int i = 0; i < roomIdArr.length; i++) {
                    roomIdByteArr[i] = (roomIdArr[i] + "").getBytes();
                }
                String tableName = getTableName((int) entry.getKey());
                connection.hashCommands().hMGet(tableName.getBytes(), roomIdByteArr);
            }
            return null;
        });
        return roomObjectList.stream().map(a -> (FriendRoom) a).toList();
    }
}
