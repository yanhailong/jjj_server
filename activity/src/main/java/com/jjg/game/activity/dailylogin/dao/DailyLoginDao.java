package com.jjg.game.activity.dailylogin.dao;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.utils.TimeHelper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lm
 * @date 2025/9/18 19:55
 */
@Repository
public class DailyLoginDao {
    private final RedisTemplate<String, String> redisTemplate;
    //类型->活动id->玩家id
    private final String REDIS_DAILY_LOGIN = "activity:dailylogin:%d:%d:%d";
    //活动id->玩家id
    private final String REDIS_DAILY_LOGIN_LOCK = "activity:dailyloginlock:%d:%d";
    //活动id->玩家id
    private final String REDIS_DAILY_LOGIN_CLAIM_TIME = "activity:dailyloginclaimtime:%d:%d";

    public DailyLoginDao(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取redis Lock
     *
     * @param playerId 玩家id
     * @return redis Lock
     */
    private String getLockKey(long activityId, long playerId) {
        return REDIS_DAILY_LOGIN_LOCK.formatted(activityId, playerId);
    }

    /**
     * 获取redis key
     *
     * @param type     type=1为持续天数 type=2为累计天数
     * @param playerId 玩家id
     * @return redis key
     */
    private String getKey(long activityId, int type, long playerId) {
        return REDIS_DAILY_LOGIN.formatted(type, activityId, playerId);
    }

    /**
     * 更新领取时间
     */
    public void updateClaimTime(long activityId, long playerId) {
        redisTemplate.opsForValue().set(REDIS_DAILY_LOGIN_CLAIM_TIME.formatted(activityId, playerId), String.valueOf(TimeHelper.getCurrentDateZeroMilliTime()));
    }

    /**
     * 获取领取时间
     */
    public long getClaimTime(long activityId, long playerId) {
        String time = redisTemplate.opsForValue().get(REDIS_DAILY_LOGIN_CLAIM_TIME.formatted(activityId, playerId));
        return time == null ? 0 : Long.parseLong(time);
    }

    /**
     * 获取累计登录天数
     */
    public int getCumulativeLoginDay(long activityId, long playerId) {
        String days = redisTemplate.opsForValue().get(getKey(activityId, ActivityConstant.DailyLogin.CUMULATIVE_TYPE, playerId));
        return days == null ? 0 : Integer.parseInt(days);
    }

    /**
     * 增加累计登录天数
     */
    public long addCumulativeLoginDay(long activityId, long playerId) {
        Long increment = redisTemplate.opsForValue().increment(getKey(activityId, ActivityConstant.DailyLogin.CUMULATIVE_TYPE, playerId), 1);
        return increment == null ? 0 : increment;
    }

    /**
     * 删除累计登录天数
     */
    public void delCumulativeLoginDay(long activityId, long playerId) {
        redisTemplate.delete(getKey(activityId, ActivityConstant.DailyLogin.CUMULATIVE_TYPE, playerId));
    }


    /**
     * 增加连续登录天数
     */
    public long addContinuousLoginDay(long activityId, long playerId) {
        Long increment = redisTemplate.opsForValue().increment(getKey(activityId, ActivityConstant.DailyLogin.CONTINUE_TYPE, playerId), 1);
        return increment == null ? 0 : increment;
    }

    /**
     * 获取连续登录天数
     */
    public long getContinuousLoginDay(long activityId, long playerId) {
        String days = redisTemplate.opsForValue().get(getKey(activityId, ActivityConstant.DailyLogin.CONTINUE_TYPE, playerId));
        return days == null ? 0 : Long.parseLong(days);
    }

    /**
     * 删除连续登录天数
     */
    public void delContinuousLoginDay(long activityId, long playerId) {
        redisTemplate.delete(getKey(activityId, ActivityConstant.DailyLogin.CONTINUE_TYPE, playerId));
    }
}
