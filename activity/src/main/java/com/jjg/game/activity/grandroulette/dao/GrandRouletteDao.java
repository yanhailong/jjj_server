package com.jjg.game.activity.grandroulette.dao;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.dao.RecordDao;
import com.jjg.game.activity.grandroulette.data.GrandRouletteSubordinateInfo;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.utils.RedisUtils;
import org.redisson.api.*;
import org.redisson.client.codec.LongCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author lm
 * @date 2025/12/3 16:47
 */
@Repository
public class GrandRouletteDao {
    private static final Logger log = LoggerFactory.getLogger(GrandRouletteDao.class);
    private static final String MODIFY_TIMES_SCRIPT = """
            local TWO_POW_32 = 4294967296
            
            local currentData = redis.call('HGET', KEYS[1], ARGV[1])
            if currentData then
                currentData = tonumber(currentData)
            else
                currentData = 0
            end
            
            -- 用除法和取模替代位运算
            local currentDrawTimes = currentData % TWO_POW_32
            local remainingTimes   = math.floor(currentData / TWO_POW_32)
            
            currentDrawTimes = currentDrawTimes + tonumber(ARGV[2])
            remainingTimes   = remainingTimes   + tonumber(ARGV[3])
            
            local newData = remainingTimes * TWO_POW_32 + currentDrawTimes
            
            redis.call('HSET', KEYS[1], ARGV[1], newData)
            return newData
            """;
    private final RedissonClient redissonClient;
    /**
     * 累计金币key
     */
    private final String BASE_GOLD_KEY = "activity:grandroulette:gold:%d";
    /**
     * 累计充值key
     */
    private final String BASE_RECHARGE_KEY = "activity:grandroulette:recharge:%d";
    /**
     * 累计次数key
     */
    private final String BASE_TIMES_KEY = "activity:grandroulette:times:%d";
    /**
     * 下级信息
     */
    private final String BASE_SUBORDINATE_KEY = "activity:grandroulette:subordinate:%d";
    /**
     * 绑定ip，mac信息 类型1ip 2mac
     */
    private final String BASE_BIND_INFO_KEY = "activity:grandroulette:bind_info:%d";
    private final RecordDao recordDao;

    public GrandRouletteDao(RedissonClient redissonClient, RecordDao recordDao) {
        this.redissonClient = redissonClient;
        this.recordDao = recordDao;
    }

    /**
     * 新增下级信息
     *
     * @param activityId    活动id
     * @param playerId      玩家id
     * @param subordinateId 下级玩家id
     * @param time          时间
     */
    public GrandRouletteSubordinateInfo addSubordinateId(long activityId, long playerId, long subordinateId, int time) {
        String key = BASE_SUBORDINATE_KEY.formatted(activityId);
        RMap<Long, GrandRouletteSubordinateInfo> map = redissonClient.getMap(key);
        RLock lock = map.getLock(playerId);
        boolean isLock = false;
        try {
            isLock = lock.tryLock(500, TimeUnit.MILLISECONDS);
            if (!isLock) {
                return null;
            }
            GrandRouletteSubordinateInfo info = map.get(playerId);
            if (info == null) {
                info = new GrandRouletteSubordinateInfo();
            }
            Map<Long, Integer> subordinateMap = info.getSubordinateMap();
            subordinateMap.put(subordinateId, time);
            map.put(playerId, info);
            return info;
        } catch (Exception e) {
            log.error("addSubordinateId:获取锁失败 activityId:{} playerId:{}  subordinateId:{} time:{}", activityId, playerId, subordinateId, time, e);
        } finally {
            if (isLock) {
                lock.unlock();
            }
        }
        return null;
    }

    /**
     * 增加被绑定人的绑定IP,MAC信息
     *
     * @param ip  绑定IP信息
     * @param mac 绑定MAC信息
     */
    public boolean addBindIpInfo(String ip, String mac) {
        String ipKey = BASE_BIND_INFO_KEY.formatted(1);
        RSet<String> ipSet = redissonClient.getSet(ipKey);
        boolean add = ipSet.add(ip);
        if (!add) {
            return false;
        }
        String macKey = BASE_BIND_INFO_KEY.formatted(2);
        RSet<String> macSet = redissonClient.getSet(macKey);
        add = macSet.add(mac);
        if (!add) {
            ipSet.remove(ip);
            return false;
        }
        return true;
    }


    /**
     * 获取下级信息
     *
     * @param activityId 活动id
     * @param playerId   玩家id
     */
    public GrandRouletteSubordinateInfo getSubordinateIds(long activityId, long playerId) {
        String key = BASE_SUBORDINATE_KEY.formatted(activityId);
        RMap<Long, GrandRouletteSubordinateInfo> map = redissonClient.getMap(key);
        return map.get(playerId);
    }

    /**
     * 增加有效金币数量
     *
     * @param activityId 活动id
     * @param playerId   玩家id
     * @param goldNum    金币数量
     */
    public void addCumulativeGold(long activityId, long playerId, long goldNum) {
        String key = BASE_GOLD_KEY.formatted(activityId);
        RMap<Long, Long> map = redissonClient.getMap(key);
        map.addAndGet(playerId, goldNum);
    }

    /**
     * 增加累计充值
     *
     * @param activityId    活动id
     * @param playerId      玩家id
     * @param rechargeValue 充值数量
     */
    public void addCumulativeRecharge(long activityId, long playerId, BigDecimal rechargeValue) {
        String key = BASE_RECHARGE_KEY.formatted(activityId);
        RMap<Long, Long> map = redissonClient.getMap(key);
        map.addAndGet(playerId, RedisUtils.toLong(rechargeValue));
    }

    /**
     * 使用 Lua 脚本原子地更新玩家的当前抽取次数和剩余次数
     *
     * @param activityId     活动id
     * @param playerId       玩家id
     * @param currentDelta   当前抽取次数变化量（例如 +1）
     * @param remainingDelta 剩余次数变化量（例如 -1）
     * @return 更新后的次数（按位合并）
     */
    public long addCumulativeTimes(long activityId, long playerId, long currentDelta, long remainingDelta) {
        String key = BASE_TIMES_KEY.formatted(activityId);
        // 执行 Lua 脚本
        long data = redissonClient.getScript(LongCodec.INSTANCE)
                .eval(RScript.Mode.READ_WRITE, MODIFY_TIMES_SCRIPT, RScript.ReturnType.INTEGER,
                        Collections.singletonList(key),
                        playerId,
                        currentDelta,
                        remainingDelta);
        // 获取高32位，即剩余次数
        return data >> 32;
    }

    /**
     * 获取玩家的当前抽取次数（低32位）和剩余次数（高32位）
     *
     * @param activityId 活动id
     * @param playerId   玩家id
     * @return 参加次数,剩余次数
     */
    public Pair<Long, Long> getPlayerTimes(long activityId, long playerId) {
        String key = BASE_TIMES_KEY.formatted(activityId);
        RMap<Long, Long> map = redissonClient.getMap(key, LongCodec.INSTANCE);
        long data = map.computeIfAbsent(playerId, addKey -> 1L << 32);
        long currentDrawTimes = data & 0xFFFFFFFFL;  // 获取低32位，即当前抽取次数
        long remainingTimes = data >> 32;  // 获取高32位，即剩余次数
        return Pair.newPair(currentDrawTimes, remainingTimes);
    }

    /**
     * 获取多个玩家的累计金币
     *
     * @param activityId 活动id
     * @param playerIds  玩家id列表
     * @return 返回一个包含玩家id和金币数的Map
     */
    public Map<Long, Long> getMultipleCumulativeGold(long activityId, Set<Long> playerIds) {
        String key = BASE_GOLD_KEY.formatted(activityId);
        RMap<Long, Long> map = redissonClient.getMap(key);

        // 批量获取所有玩家的数据，减少与 Redis 的交互次数
        Map<Long, Long> result = map.getAll(playerIds);

        // 如果 Redis 中没有找到玩家的数据，返回默认值 0
        for (Long playerId : playerIds) {
            result.putIfAbsent(playerId, 0L);
        }
        return result;
    }

    /**
     * 获取多个玩家的累计充值
     *
     * @param activityId 活动id
     * @param playerIds  玩家id列表
     * @return 返回一个包含玩家id和充值数的Map
     */
    public Map<Long, Long> getMultipleCumulativeRecharge(long activityId, Set<Long> playerIds) {
        String key = BASE_RECHARGE_KEY.formatted(activityId);
        RMap<Long, Long> map = redissonClient.getMap(key);

        // 批量获取所有玩家的数据，减少与 Redis 的交互次数
        Map<Long, Long> result = map.getAll(playerIds);

        // 如果 Redis 中没有找到玩家的数据，返回默认值 0
        for (Long playerId : playerIds) {
            result.putIfAbsent(playerId, 0L);
        }
        return result;
    }


    /**
     * 根据玩家id重置玩家数据
     *
     * @param activityId 活动id
     * @param playerId   玩家id
     */
    public void resetPlayerActivityData(long activityId, long playerId) {
        //删除次数
        String key = BASE_TIMES_KEY.formatted(activityId);
        redissonClient.getMap(key, LongCodec.INSTANCE).fastRemove(playerId);

        //移除下级信息
        String subordinateKey = BASE_SUBORDINATE_KEY.formatted(activityId);
        GrandRouletteSubordinateInfo remove = redissonClient.<Long, GrandRouletteSubordinateInfo>getMap(subordinateKey).remove(playerId);
        if (remove == null || CollectionUtil.isEmpty(remove.getSubordinateMap())) {
            return;
        }
        Long[] ids = remove.getSubordinateMap().keySet().toArray(Long[]::new);

        // 移除金币数据
        String goldKey = BASE_GOLD_KEY.formatted(activityId);
        redissonClient.<Long, Long>getMap(goldKey).fastRemove(ids);

        // 移除充值数据
        String rechargeKey = BASE_RECHARGE_KEY.formatted(activityId);
        redissonClient.<Long, Long>getMap(rechargeKey).fastRemove(ids);
    }


    /**
     * 重置活动的所有玩家数据
     *
     * @param activityId 活动id
     * @param prefix     前缀
     */
    public void resetActivityData(long activityId, String prefix) {
        String key = BASE_TIMES_KEY.formatted(activityId);
        redissonClient.getMap(key, LongCodec.INSTANCE).clear();
        // 重置金币数据
        String goldKey = BASE_GOLD_KEY.formatted(activityId);
        redissonClient.getMap(goldKey).clear();
        // 重置充值数据
        String rechargeKey = BASE_RECHARGE_KEY.formatted(activityId);
        redissonClient.getMap(rechargeKey).clear();
        // 重置下级数据
        String subordinateKey = BASE_SUBORDINATE_KEY.formatted(activityId);
        redissonClient.getMap(subordinateKey).clear();

        //重置记录
        recordDao.deleteAllRecords(prefix, activityId);
        recordDao.deleteAllPlayerRecords(prefix, activityId);
    }
}