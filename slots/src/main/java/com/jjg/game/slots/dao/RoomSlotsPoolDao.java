package com.jjg.game.slots.dao;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.service.SlotsPlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class RoomSlotsPoolDao extends AbstractPoolDao {
    @Autowired
    private SlotsPlayerService slotsPlayerService;

    /**
     * 初始化房间水池
     * @param roomId
     * @param reserveValue
     */
    public void initRoomPool(long roomId,long reserveValue){
        String tableName = roomPoolTableName(roomId);

        this.redisTemplate.opsForHash().putIfAbsent(tableName, SlotsConst.RoomSlotsPool.TYPE_STANDARD, reserveValue);
        this.redisTemplate.opsForHash().putIfAbsent(tableName, SlotsConst.RoomSlotsPool.TYPE_ALL_REVERSE, reserveValue);
        this.redisTemplate.opsForHash().putIfAbsent(tableName, SlotsConst.RoomSlotsPool.TYPE_ALL_INCOME, 0);
    }

    /**
     * 给房间添加准备金
     *
     * @param roomId
     * @param addReserveValue 添加的准备金
     * @return
     */
    public long[] addReserve(long roomId, long addReserveValue) {
        if (addReserveValue < 0) {
            return null;
        }

        String tableName = roomPoolTableName(roomId);

        long afterStandardValue = this.redisTemplate.opsForHash().increment(tableName, SlotsConst.RoomSlotsPool.TYPE_STANDARD, addReserveValue);
        long afterReserveValue = this.redisTemplate.opsForHash().increment(tableName, SlotsConst.RoomSlotsPool.TYPE_ALL_REVERSE, addReserveValue);
        return new long[]{afterStandardValue, afterReserveValue};
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
        return this.redisTemplate.opsForHash().increment(roomPoolTableName(roomId), SlotsConst.RoomSlotsPool.TYPE_STANDARD, value);
    }

    /**
     * 给收益池加钱
     *
     * @param roomId
     * @param value
     */
    public Long addToReversePool(long roomId, long value) {
        if (value < 1) {
            return null;
        }
        return this.redisTemplate.opsForHash().increment(roomPoolTableName(roomId), SlotsConst.RoomSlotsPool.TYPE_ALL_INCOME, value);
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
}
