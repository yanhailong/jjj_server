package com.jjg.game.activity.dailylogin.dao;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.redis.PlayerRedis;
import com.jjg.game.common.utils.TimeHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lm
 * @date 2025/9/18 19:55
 */
@Repository
public class DailyLoginDao {
    private final RedisTemplate<String, String> redisTemplate;
    private final PlayerRedis playerRedis;
    //类型->活动id->玩家id
    private final String REDIS_DAILY_LOGIN = "activity:dailylogin:%d:%d";
    //活动id->玩家id
    private final String REDIS_DAILY_LOGIN_CLAIM_TIME = "activity:dailyloginclaimtime:%d";

    public DailyLoginDao(RedisTemplate<String, String> redisTemplate, PlayerRedis playerRedis) {
        this.redisTemplate = redisTemplate;
        this.playerRedis = playerRedis;
    }

    /**
     * 获取redis key
     *
     * @param type type=1为持续天数 type=2为累计天数
     * @return redis key
     */
    private String getKey(long activityId, int type) {
        return REDIS_DAILY_LOGIN.formatted(type, activityId);
    }

    /**
     * 更新领取时间
     */
    public void updateClaimTime(long activityId, long playerId) {
        playerRedis.hset(playerId,
                REDIS_DAILY_LOGIN_CLAIM_TIME.formatted(activityId),
                String.valueOf(playerId),
                String.valueOf(TimeHelper.getCurrentDateZeroMilliTime()));
    }

    private HashOperations<String, String, String> getOpsForHash() {
        return redisTemplate.opsForHash();
    }

    /**
     * 获取领取时间
     */
    public long getClaimTime(long activityId, long playerId) {
        String obj = getOpsForHash().get(REDIS_DAILY_LOGIN_CLAIM_TIME.formatted(activityId), String.valueOf(playerId));
        if (obj == null) {
            return 0;
        }
        return Long.parseLong(obj);
    }

    /**
     * 获取累计登录天数
     */
    public int getCumulativeLoginDay(long activityId, long playerId) {
        String days = getOpsForHash().get(getKey(activityId, ActivityConstant.DailyLogin.CUMULATIVE_TYPE), String.valueOf(playerId));
        if (StringUtils.isEmpty(days)) {
            return 0;
        }
        return Integer.parseInt(days);
    }

    /**
     * 增加累计登录天数
     */
    public long addCumulativeLoginDay(long activityId, long playerId) {
        return playerRedis.hincr(playerId, getKey(activityId, ActivityConstant.DailyLogin.CUMULATIVE_TYPE), String.valueOf(playerId), 1);
    }

    /**
     * 删除累计登录天数
     */
    public void delCumulativeLoginDay(long activityId, long playerId) {
        playerRedis.hdelete(playerId, getKey(activityId, ActivityConstant.DailyLogin.CUMULATIVE_TYPE), String.valueOf(playerId));
    }


    /**
     * 增加连续登录天数
     */
    public long addContinuousLoginDay(long activityId, long playerId) {
        return playerRedis.hincr(playerId, getKey(activityId, ActivityConstant.DailyLogin.CONTINUE_TYPE), String.valueOf(playerId), 1);
    }

    /**
     * 获取连续登录天数
     */
    public long getContinuousLoginDay(long activityId, long playerId) {
        String obj = getOpsForHash().get(getKey(activityId, ActivityConstant.DailyLogin.CONTINUE_TYPE), String.valueOf(playerId));
        if (obj == null) {
            return 0;
        }
        return Integer.parseInt(obj);
    }

    /**
     * 删除连续登录天数
     */
    public void delContinuousLoginDay(long activityId, long playerId) {
        playerRedis.hdelete(playerId, getKey(activityId, ActivityConstant.DailyLogin.CONTINUE_TYPE), String.valueOf(playerId));
    }
}
