package com.jjg.game.slots.dao;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.slots.service.SlotsPlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Component
public class RoomSlotsPoolDao extends AbstractPoolDao {
    @Autowired
    private SlotsPlayerService slotsPlayerService;

    /**
     * 初始化房间水池
     *
     * @param roomId
     * @param reserveValue
     */
    public void initRoomPool(long roomId, long reserveValue) {
        this.redisTemplate.opsForHash().putIfAbsent(room_pool_prefix, roomId, reserveValue);
    }

    /**
     * 根据房间id获取池子
     *
     * @return
     */
    public Number getBigPoolByRoomId(long roomId) {
        return (Number) this.redisTemplate.opsForHash().get(room_pool_prefix, roomId);
    }

    public Map<Long, Long> getPools(List<Long> roomIds) {
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long roomId : roomIds) {
                connection.hGet(room_pool_prefix.getBytes(), String.valueOf(roomId).getBytes());
            }
            return null;
        });

        Map<Long, Long> resultMap = new HashMap<>();
        for (int i = 0; i < roomIds.size(); i++) {
            Object value = results.get(i);
            if (value != null) {
                resultMap.put(roomIds.get(i), Long.parseLong(value.toString()));
            }
        }
        return resultMap;
    }

    /**
     * 给标准池子加钱,池子可以为负
     *
     * @param roomId
     * @param value
     */
    public Long addToBigPool(long roomId, long value) {
        if (value == 0) {
            return null;
        }
        return this.redisTemplate.opsForHash().increment(room_pool_prefix, roomId, value);
    }

    /**
     * 从标准池扣钱，然后给玩家加钱
     *
     * @param playerId
     * @param roomId
     * @param roomId
     * @param value
     * @param addType
     * @return
     */
    public CommonResult<Player> rewardFromBigPool(long playerId, long roomId, long value, AddType addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        if (value == 0) {
            result.code = Code.PARAM_ERROR;
            return result;
        }
        value = Math.abs(value);

        Long after = addToBigPool(roomId, -value);
        if (after == null) {
            result.code = Code.FAIL;
            return result;
        }

        result = slotsPlayerService.addGold(playerId, value, addType);
        if (!result.success()) {  //如果失败，要把钱重新加回池子
            addToBigPool(roomId, value);
            return result;
        }
//        log.debug("从标准池扣除，并给玩家加钱成功 playerId = {},gameType = {},roomCfgId = {},value = {},afterPool = {},addType = {}", playerId, gameType, roomCfgId, value, after, addType);
        return result;
    }

    public void removePoolByRoomId(long roomId) {
        this.redisTemplate.opsForHash().delete(room_pool_prefix,roomId);
    }
}
