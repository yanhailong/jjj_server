package com.jjg.game.slots.dao;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.slots.service.SlotsPlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


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
     * @param value
     * @param transactionItemId
     * @param addType
     * @return 玩家信息,奖池剩余
     */
    public CommonResult<Pair<Player, Long>> rewardFromBigPool(long playerId, long roomId, long value, int transactionItemId, AddType addType) {
        CommonResult<Pair<Player, Long>> result = new CommonResult<>(Code.SUCCESS);
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
        CommonResult<Player> playerResult = slotsPlayerService.addCurrent(playerId, value, transactionItemId, addType, String.valueOf(roomId), false);
        if (!playerResult.success()) {  //如果失败，要把钱重新加回池子
            addToBigPool(roomId, value);
            result.code = playerResult.code;
            return result;
        }
//        log.debug("从标准池扣除，并给玩家加钱成功 playerId = {},gameType = {},roomCfgId = {},value = {},afterPool = {},addType = {}", playerId, gameType, roomCfgId, value, after, addType);
        result.data = new Pair<>(playerResult.data, after);
        return result;
    }

    public void removePoolByRoomId(long roomId) {
        this.redisTemplate.opsForHash().delete(room_pool_prefix, roomId);
    }
}
