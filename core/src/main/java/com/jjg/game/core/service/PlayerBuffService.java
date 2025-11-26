package com.jjg.game.core.service;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerBuff;
import com.jjg.game.core.data.PlayerBuffDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author 11
 * @date 2025/8/19 11:11
 */
@Service
public class PlayerBuffService {
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected final String tableName = "playerBuff";
    private final String lockTableName = "lockplayerBuff:";

    @Autowired
    protected RedisTemplate<String, Player> redisTemplate;
    @Autowired
    protected RedisLock redisLock;

    protected BigDecimal tenThousandBigDecimal = BigDecimal.valueOf(10000);

    protected String getLockKey(long playerId) {
        return lockTableName + playerId;
    }

    /**
     * 添加buff
     * @param playerId
     * @param buffType
     * @param value
     * @param expireTime
     * @return
     */
    public CommonResult<PlayerBuff> addBuff(long playerId, int buffType, int value, int expireTime) {
        CommonResult<PlayerBuff> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);

        int expire = TimeHelper.nowInt() + expireTime;
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.debug("获取锁失败 lockKey:{} playerId = {} buffType:{} value:{} expireTime:{}", key, playerId, buffType, value, expireTime);
                return result;
            }
            PlayerBuff playerBuff = get(playerId);
            if (playerBuff == null) {
                playerBuff = new PlayerBuff();
                Map<Integer, List<PlayerBuffDetail>> details = new HashMap<>();
                playerBuff.setDetails(details);
                playerBuff.setPlayerId(playerId);
            }

            List<PlayerBuffDetail> playerBuffDetails = playerBuff.getDetails().computeIfAbsent(buffType, k -> new ArrayList<>());
            PlayerBuffDetail detail = new PlayerBuffDetail();
            detail.setType(buffType);
            detail.setValue(value);
            detail.setExpire(expire);
            playerBuffDetails.add(detail);

            redisTemplate.opsForHash().put(tableName, playerId, playerBuff);
            result.code = Code.SUCCESS;
            result.data = playerBuff;
            return result;
        } catch (Exception e) {
            log.warn("创建或保存玩家buff对象异常 playerId={}", playerId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(key);
            }
        }
        return result;
    }

    /**
     * 获取playerbuff对象，检查过期
     * @param playerId
     * @return
     */
    public PlayerBuff get(long playerId) {
        HashOperations<String, String, PlayerBuff> operations = redisTemplate.opsForHash();
        String key = getLockKey(playerId);

        int now = TimeHelper.nowInt();
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.debug("获取锁失败 lockKey:{} playerId = {} ", key, playerId);
                return null;
            }
            PlayerBuff playerBuff = operations.get(tableName, playerId);
            if (playerBuff == null) {
                return playerBuff;
            }

            if (playerBuff.getDetails() == null || playerBuff.getDetails().isEmpty()) {
                return playerBuff;
            }

            Iterator<Map.Entry<Integer, List<PlayerBuffDetail>>> it = playerBuff.getDetails().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, List<PlayerBuffDetail>> en = it.next();
                List<PlayerBuffDetail> list = en.getValue();
                if (list == null || list.isEmpty()) {
                    it.remove();
                    continue;
                }
                list.removeIf(detail -> detail.getExpire() < now);
            }
            return playerBuff;
        } catch (Exception e) {
            log.warn("获取玩家buff对象异常 playerId={}", playerId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(key);
            }
        }
        return null;
    }

    public BigDecimal calProp(int baseProp, List<PlayerBuffDetail> details) {
        int allProp = baseProp;
        if (details != null && !details.isEmpty()) {
            for (PlayerBuffDetail d : details) {
                allProp += d.getValue();
            }
        }
        return BigDecimal.valueOf(allProp).divide(tenThousandBigDecimal, 2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal calProp(BigDecimal beforeProp, int addProp) {
        if (addProp < 1) {
            return beforeProp;
        }

        return beforeProp.add(BigDecimal.valueOf(addProp).divide(tenThousandBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
    }
}
