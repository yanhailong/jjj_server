package com.jjg.game.slots.dao;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.slots.sample.GameDataManager;
import com.jjg.game.slots.sample.bean.BaseRoomCfg;
import com.jjg.game.slots.service.SlotsPlayerService;
import com.jjg.game.slots.utils.SlotsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * @author 11
 * @date 2025/7/10 13:36
 */
@Component
public class SlotsPoolDao extends AbstractPoolDao {

    @Autowired
    private SlotsPlayerService slotsPlayerService;
    protected BigDecimal tenThousandBigDecimal = BigDecimal.valueOf(10000);

    /**
     * 初始化水池
     */
    @Override
    public void initPool() {
        for (Map.Entry<Integer, BaseRoomCfg> en : GameDataManager.getBaseRoomCfgMap().entrySet()) {
            BaseRoomCfg cfg = en.getValue();
            this.redisTemplate.opsForHash().putIfAbsent(tableName(cfg.getGameType()), cfg.getRoomName(), cfg.getInitBasePool());
            this.redisTemplate.opsForHash().putIfAbsent(smallTableName(cfg.getGameType()), cfg.getRoomName(), 0);
            this.redisTemplate.opsForHash().putIfAbsent(fakeSmallTableName(cfg.getGameType()), cfg.getRoomName(), cfg.getFakePool());
        }
    }

    /**
     * 给标准池子加钱,池子可以为负
     *
     * @param roomName
     * @param value
     */
    public Long addToBigPool(int gameType, int roomName, long value) {
        if (value == 0) {
            return null;
        }
        return this.redisTemplate.opsForHash().increment(tableName(gameType), roomName, value);
    }

    /**
     * 给小池子池子加钱,池子可以为负
     *
     * @param roomName
     * @param value
     */
    public Long addToSmallPool(int gameType, int roomName, long value) {
        if (value == 0) {
            return null;
        }

        //当前真奖池金额
        long poolValue = this.redisTemplate.opsForHash().increment(smallTableName(gameType), roomName, value);
        //获取假奖池金额
        Number fakePoolValue = (Number)this.redisTemplate.opsForHash().get(fakeSmallTableName(gameType), roomName);
        if(fakePoolValue == null) {
            return poolValue;
        }

        BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(SlotsUtil.wareIdToRoomCfgId(roomName, gameType));
        if(baseRoomCfg == null || baseRoomCfg.getFakeCommissionProp() == null || baseRoomCfg.getFakeCommissionProp().size() < 3) {
            return poolValue;
        }

        long poolDiff = fakePoolValue.longValue() - poolValue;
        if(poolDiff > baseRoomCfg.getFakeCommissionProp().get(0)){
            BigDecimal prop = BigDecimal.valueOf(baseRoomCfg.getFakeCommissionProp().get(1)).divide(tenThousandBigDecimal,4, RoundingMode.HALF_UP);
            long addToFakeValue = BigDecimal.valueOf(value).multiply(prop).longValue();
            this.redisTemplate.opsForHash().increment(fakeSmallTableName(gameType), roomName, addToFakeValue);
            log.debug("添加到假奖池 gameType = {},wareId = {},addToFakeValue = {}", gameType, roomName, addToFakeValue);
        }else {
            BigDecimal prop = BigDecimal.valueOf(baseRoomCfg.getFakeCommissionProp().get(2)).divide(tenThousandBigDecimal,4, RoundingMode.HALF_UP);
            long addToFakeValue = BigDecimal.valueOf(value).multiply(prop).longValue();
            this.redisTemplate.opsForHash().increment(fakeSmallTableName(gameType), roomName, addToFakeValue);
            log.debug("添加到假奖池 gameType = {},wareId = {},addToFakeValue = {}", gameType, roomName, addToFakeValue);
        }
        return poolValue;
    }

    /**
     * 清空小池子并且返回
     *
     * @param gameType
     * @param roomName
     * @return
     */
    public Long clearSmallPool(int gameType, int roomName) {
        String tableName = tableName(gameType);
        BoundHashOperations<String, String, Long> ops = redisTemplate.boundHashOps(tableName);
        Long oldValue = ops.get(roomName);
        if (oldValue == null || oldValue < 1) {
            return 0L;
        }
        ops.put(roomName + "", 0L);
        return oldValue;
    }

    /**
     * 从标准池扣钱，然后给玩家加钱
     *
     * @param playerId
     * @param gameType
     * @param roomName
     * @param value
     * @param addType
     * @return
     */
    public CommonResult<Player> rewardFromBigPool(long playerId, int gameType, int roomName, long value, String addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        if (value == 0) {
            result.code = Code.PARAM_ERROR;
            return result;
        }
        value = Math.abs(value);

        Long after = addToBigPool(gameType, roomName, -value);
        if (after == null) {
            result.code = Code.FAIL;
            return result;
        }

        result = slotsPlayerService.addGold(playerId, value, addType);
        if (!result.success()) {  //如果失败，要把钱重新加回池子
            addToBigPool(gameType, roomName, value);
            return result;
        }

        log.debug("从标准池扣除，并给玩家加钱成功 playerId = {},gameType = {},roomName = {},value = {},afterPool = {},addType = {}", playerId, gameType, roomName, value, after, addType);
        return result;
    }

    /**
     * 从小池子扣钱，然后给玩家加钱
     *
     * @param playerId
     * @param gameType
     * @param roomName
     * @param addType
     * @return
     */
    public CommonResult<Player> rewardFromSmallPool(long playerId, int gameType, int roomName, String addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);

        long poolValue = clearSmallPool(gameType, roomName);
        if (poolValue < 1) {
            result.code = Code.FAIL;
            return result;
        }

        result = slotsPlayerService.addGold(playerId, poolValue, addType);
        if (!result.success()) {  //如果失败，要把钱重新加回池子
            addToSmallPool(gameType, roomName, poolValue);
            return result;
        }

        log.debug("从小池子扣除，并给玩家加钱成功 playerId = {},gameType = {},roomName = {},smallPoolValue = {},addType = {}", playerId, gameType, roomName, poolValue, addType);
        return result;
    }
}
