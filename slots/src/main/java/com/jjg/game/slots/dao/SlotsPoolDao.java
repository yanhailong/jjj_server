package com.jjg.game.slots.dao;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.task.param.TaskConditionParam10003;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.slots.service.SlotsPlayerService;
import com.jjg.game.slots.utils.SlotsUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private TaskManager taskManager;
    protected BigDecimal tenThousandBigDecimal = BigDecimal.valueOf(10000);

    /**
     * 初始化水池
     */
    @Override
    public void initPool() {
        for (Map.Entry<Integer, BaseRoomCfg> en : GameDataManager.getBaseRoomCfgMap().entrySet()) {
            BaseRoomCfg cfg = en.getValue();
            this.redisTemplate.opsForHash().putIfAbsent(tableName(cfg.getGameType()), cfg.getId(), cfg.getInitBasePool());
            this.redisTemplate.opsForHash().putIfAbsent(smallTableName(cfg.getGameType()), cfg.getId(), 0);
            this.redisTemplate.opsForHash().putIfAbsent(fakeSmallTableName(cfg.getGameType()), cfg.getId(), cfg.getFakePool());
        }
    }

    /**
     * 给标准池子加钱,池子可以为负
     *
     * @param roomCfgId
     * @param value
     */
    public Long addToBigPool(int gameType, int roomCfgId, long value) {
        if (value == 0) {
            return null;
        }
        return this.redisTemplate.opsForHash().increment(tableName(gameType), roomCfgId, value);
    }

    /**
     * 给小池子池子加钱,池子可以为负
     *
     * @param roomCfgId
     * @param value
     */
    public Long addToSmallPool(int gameType, int roomCfgId, long value) {
        if (value == 0) {
            return null;
        }

        //当前真奖池金额
        long poolValue = this.redisTemplate.opsForHash().increment(smallTableName(gameType), roomCfgId, value);
        if (value > 0) { //给池子加金币
            //获取假奖池金额
            Number fakePoolValue = (Number) this.redisTemplate.opsForHash().get(fakeSmallTableName(gameType), roomCfgId);
            if (fakePoolValue == null) {
                return poolValue;
            }

            BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(roomCfgId);
            if (baseRoomCfg == null || baseRoomCfg.getFakeCommissionProp() == null || baseRoomCfg.getFakeCommissionProp().size() < 3) {
                return poolValue;
            }

            long poolDiff = fakePoolValue.longValue() - poolValue;
            if (poolDiff > baseRoomCfg.getFakeCommissionProp().get(0)) {
                BigDecimal prop = BigDecimal.valueOf(baseRoomCfg.getFakeCommissionProp().get(1)).divide(tenThousandBigDecimal, 4, RoundingMode.HALF_UP);
                long addToFakeValue = BigDecimal.valueOf(value).multiply(prop).longValue();
                long afterValue = this.redisTemplate.opsForHash().increment(fakeSmallTableName(gameType), roomCfgId, addToFakeValue);
                log.info("添加到假奖池1 gameType = {},roomCfgId = {},addToPoolValue = {},addToFakeValue = {},afterValue = {}", gameType, roomCfgId, value, addToFakeValue, afterValue);
            } else {
                BigDecimal prop = BigDecimal.valueOf(baseRoomCfg.getFakeCommissionProp().get(2)).divide(tenThousandBigDecimal, 4, RoundingMode.HALF_UP);
                long addToFakeValue = BigDecimal.valueOf(value).multiply(prop).longValue();
                long afterValue = this.redisTemplate.opsForHash().increment(fakeSmallTableName(gameType), roomCfgId, addToFakeValue);
                log.debug("添加到假奖池2 gameType = {},roomCfgId = {},addToPoolValue = {},addToFakeValue = {},afterValue = {}", gameType, roomCfgId, value, addToFakeValue, afterValue);
            }
        } else {  //从池子扣除
            Number fakePoolValue = this.redisTemplate.opsForHash().increment(fakeSmallTableName(gameType), roomCfgId, value);
            log.debug("从小奖池扣除成功 gameType = {},roomCfgId = {},value = {},afterPoolValue = {},afterFakePoolValue = {}", gameType, roomCfgId, value, poolValue, fakePoolValue.longValue());
        }
        return poolValue;
    }


    /**
     * 从标准池扣钱，然后给玩家加钱
     *
     * @param playerId
     * @param gameType
     * @param roomCfgId
     * @param value
     * @param addType
     * @return
     */
    public CommonResult<Player> rewardFromBigPool(long playerId, int gameType, int roomCfgId, long value, AddType addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        if (value == 0) {
            result.code = Code.PARAM_ERROR;
            return result;
        }
        value = Math.abs(value);

        Long after = addToBigPool(gameType, roomCfgId, -value);
        if (after == null) {
            result.code = Code.FAIL;
            return result;
        }

        result = slotsPlayerService.addGold(playerId, value, addType);
        if (!result.success()) {  //如果失败，要把钱重新加回池子
            addToBigPool(gameType, roomCfgId, value);
            return result;
        }
        log.debug("从标准池扣除，并给玩家加钱成功 playerId = {},gameType = {},roomCfgId = {},value = {},afterPool = {},addType = {}", playerId, gameType, roomCfgId, value, after, addType);
        return result;
    }

    /**
     * 从小池子扣钱，然后给玩家加钱
     */
    public CommonResult<Player> rewardFromSmallPool(long playerId, int gameType, int roomCfgId, long value, AddType addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);

        Long poolValue = addToSmallPool(gameType, roomCfgId, -value);
        if (poolValue == null) {
            result.code = Code.FAIL;
            return result;
        }

        result = slotsPlayerService.addGold(playerId, poolValue, addType, desc);
        if (!result.success()) {  //如果失败，要把钱重新加回池子
            addToSmallPool(gameType, roomCfgId, value);
            return result;
        }
        log.debug("从小池子扣除，并给玩家加钱成功 playerId = {},gameType = {},roomCfgId = {},addValue = {},afterValue = {},addType = {}", playerId, gameType, roomCfgId, value, poolValue, addType);
        return result;
    }

    /**
     * 按照奖池的百分比奖励给玩家
     *
     * @param playerId
     * @param gameType
     * @param roomCfgId
     * @param ratio     奖池万分比
     * @param addType
     * @return
     */
    public CommonResult<Long> rewardByRatioFromSmallPool(long playerId, int gameType, int roomCfgId, int ratio, AddType addType) {
        CommonResult<Long> result = new CommonResult<>(Code.SUCCESS);

        Number poolValue = getSmallPoolByRoomCfgId(gameType, roomCfgId);

        long value = SlotsUtil.calProp(ratio, poolValue.longValue());
        if (value < 1) {
            result.code = Code.FAIL;
            log.debug("计算出的 value 小于1 playerId = {},gameType = {},roomCfgId = {}", playerId, gameType, roomCfgId);
            return result;
        }

        Long afterPoolValue = addToSmallPool(gameType, roomCfgId, -value);
        if (afterPoolValue == null) {
            result.code = Code.FAIL;
            return result;
        }

        CommonResult<Player> addResult = slotsPlayerService.addGold(playerId, value, addType);
        if (!addResult.success()) {  //如果失败，要把钱重新加回池子
            result.code = Code.FAIL;
            addToSmallPool(gameType, roomCfgId, value);
            log.debug("操作失败，已将金币加回池子 playerId = {},gameType = {},roomCfgId = {}", playerId, gameType, roomCfgId);
            return result;
        }

        result.data = value;
        log.debug("从小池子按照百分比扣除，并给玩家加钱成功 playerId = {},gameType = {},roomCfgId = {},beforeValue = {},addValue = {},afterValue = {},addType = {}", playerId, gameType, roomCfgId, poolValue, value, afterPoolValue, addType);
        return result;
    }
}
