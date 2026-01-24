package com.jjg.game.slots.dao;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.slots.service.SlotsPlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;


@Component
public class RoomSlotsPoolDao extends AbstractPoolDao {
    @Autowired
    private SlotsPlayerService slotsPlayerService;

    String script = """
                local value = redis.call('HGET', KEYS[1], ARGV[1])
                if value then
                    -- 先删除字段
                    redis.call('HDEL', KEYS[1], ARGV[1])
            
                    -- 尝试将值转换为数字
                    local number_value = tonumber(value)
                    if number_value then
                        -- 如果是数字，返回数字
                        return number_value
                    else
                        -- 如果不是数字，返回nil表示类型不符合要求
                        return nil
                    end
                else
                    -- 字段不存在，返回nil
                    return nil
                end
            """;

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
            log.warn("从房间奖池扣除失败 playerId = {}，roomId = {},value = {}", playerId, roomId, value);
            return result;
        }

        //如果奖池为负数,将扣除的钱加回奖池
        if (after < 0) {
            addToBigPool(roomId, value);
            result.code = Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT;
            log.warn("房间奖池扣除后为负，所以将钱加回奖池 playerId = {}，roomId = {},value = {}", playerId, roomId, value);
            return result;
        }

        CommonResult<Player> playerResult = slotsPlayerService.addCurrent(playerId, value, transactionItemId, addType, String.valueOf(roomId), false);
        if (!playerResult.success()) {  //如果失败，要把钱重新加回池子
            addToBigPool(roomId, value);
            result.code = playerResult.code;
            return result;
        }
        log.info("从标准池扣除，并给玩家加钱成功 playerId = {},roomId = {},value = {},afterPool = {},addType = {}", playerId, roomId, value, after, addType);
        result.data = new Pair<>(playerResult.data, after);
        return result;
    }

    /**
     * 删除房间池子，并返回删除的值
     * @param roomId
     * @return
     */
    public long removePoolByRoomId(long roomId) {
        // 创建RedisScript对象
        DefaultRedisScript<Number> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        // 注意：由于Lua返回的可能是整数或浮点数，使用Number作为返回类型
        redisScript.setResultType(Number.class);
        // 执行脚本
        Number deletedValue = redisTemplate.execute(redisScript, Collections.singletonList(room_pool_prefix), roomId);

        // 使用结果
        if (deletedValue != null) {
            return deletedValue.longValue();
        }
        return 0;
    }
}
