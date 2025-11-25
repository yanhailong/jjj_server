package com.jjg.game.hall.friendroom.dao;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 好友房RedisDao,重置次数记录
 *
 * @author 2CL
 */
@Repository
public class FriendRoomRedisDao {
    // logger
    private static final Logger log = LoggerFactory.getLogger(FriendRoomRedisDao.class);

    // 最大的code
    private static final int MAX_CODE = 9999_9999;
    // code掩码
    private static final int CODE_MASK = MAX_CODE - TimeHelper.ONE_DAY_OF_MILLIS;
    // 邀请码重置记录 TableName + "按天的时间" 玩家ID + 邀请码重置次数
    private static final String INVITATION_RESET_TABLE_NAME = "InvitationCodeResetRec";
    // 邀请码对应的玩家ID 邀请码 <=> 玩家ID
    private static final String INVITATION_CODE_OF_PLAYER = "InvitationCodeOfPlayer";
    // 玩家好友房屏蔽玩家ID列表 玩家ID <=> 屏蔽的玩家ID列表
    private static final String PLAYER_BLACK_LIST = "PlayerBlackList";

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate<String, List<Long>> blackListRedisTemplate;
    @Autowired
    private RedisLock redisLock;
    // 表名缓存，防止重复判断。如果是重新开服，此值会清空，但是也只是让key值多存一点时间，不影响其他逻辑
    private final Set<String> tableNameCache = new ConcurrentHashSet<>();

    /**
     * 获取一个邀请码
     */
    public int genInvitationCode() {
        String invitationPlayerTableName = getInvitationPlayerTableName();
        boolean lock = false;
        try {
            lock = redisLock.tryLock(invitationPlayerTableName, 200);
            if(!lock){
                return 0;
            }
            int invitationCode = Integer.MIN_VALUE, tryTimes = 5;
            while (tryTimes-- > 0) {
                long curTime = System.currentTimeMillis();
                long currentDateZeroMileTime = TimeHelper.getCurrentDateZeroMileTime();
                // 如果想让邀请码最低从 1000_0000 开始，将随机值的最低位设置为 1000_0000
                int maskData = RandomUtils.randomMinMax(0, CODE_MASK);
                // 是否需要判断重复创建的问题
                invitationCode = (int) (curTime - currentDateZeroMileTime + maskData);
                if (!existInvitationCode(invitationCode)) {
                    break;
                } else {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            log.info("生成邀请码：{}", invitationCode);
            return invitationCode;
        } catch (Exception e){
            log.error("",e);
        }finally {
            if(lock){
                redisLock.tryUnlock(invitationPlayerTableName);
            }
        }
        return 0;
    }

    /**
     * 获取邀请码重置表名
     */
    private String getInvitationResetTableName() {
        int dayNumerical = TimeHelper.getDayNumerical();
        return INVITATION_RESET_TABLE_NAME + dayNumerical;
    }

    /**
     * 获取邀请码对应玩家ID
     */
    private String getInvitationPlayerTableName() {
        return INVITATION_CODE_OF_PLAYER;
    }

    /**
     * 获取玩家重置次数
     */
    public Integer getInvitationCodeResetUseTimes(long playerId) {
        String tableName = getInvitationResetTableName();
        return (Integer) redisTemplate.opsForHash().get(tableName, playerId);
    }

    /**
     * 添加邀请码和玩家ID映射
     */
    public void addInvitationCode(int invitationCode, Long playerId) {
        String invitationPlayerTableName = getInvitationPlayerTableName();
        redisTemplate.opsForHash().put(invitationPlayerTableName, invitationCode, playerId);
    }

    /**
     * 通过邀请码获取玩家ID
     */
    public Number getPlayerIdByInvitationCode(int invitationCode) {
        String invitationPlayerTableName = getInvitationPlayerTableName();
        return (Number) redisTemplate.opsForHash().get(invitationPlayerTableName, invitationCode);
    }

    /**
     * 是否存在邀请码
     *
     * @return 是否存在
     */
    public boolean existInvitationCode(int invitationCode) {
        String invitationPlayerTableName = getInvitationPlayerTableName();
        return redisTemplate.opsForHash().hasKey(invitationPlayerTableName, invitationCode);
    }

    /**
     * 重置邀请码
     *
     * @param oldInvitationCode 旧的邀请码
     * @param newInvitationCode 新的邀请码
     * @param playerId          玩家ID
     */
    public void resetInvitationCode(int oldInvitationCode, int newInvitationCode, long playerId) {
        String tableName = getInvitationResetTableName();
        String invitationPlayerTableName = getInvitationPlayerTableName();
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // 添加使用次数
            connection.hashCommands().hIncrBy(tableName.getBytes(), (playerId + "").getBytes(), 1);
            // 删除旧的邀请码
            connection.hashCommands().hDel(invitationPlayerTableName.getBytes(), (oldInvitationCode + "").getBytes());
            // 添加新的邀请码
            connection.hashCommands().hSet(
                invitationPlayerTableName.getBytes(), (newInvitationCode + "").getBytes(), (playerId + "").getBytes());
            return null;
        });
        // 设置key的过期时间
        if (!tableNameCache.contains(tableName)) {
            if (redisTemplate.getExpire(tableName) <= 0) {
                log.info("过期时间：{}", TimeHelper.ONE_DAY_OF_MILLIS + TimeHelper.ONE_HOUR_OF_MILLIS);
                // 设置过期时间
                redisTemplate.expire(
                    tableName, TimeHelper.ONE_DAY_OF_MILLIS + TimeHelper.ONE_HOUR_OF_MILLIS, TimeUnit.MILLISECONDS);
                tableNameCache.add(tableName);
            }
        }
    }

    /**
     * 获取玩家黑名单列表
     */
    public List<Long> getPlayerBlackList(long playerId) {
        return (List<Long>) blackListRedisTemplate.opsForHash().get(PLAYER_BLACK_LIST, playerId);
    }

    /**
     * 更新玩家屏蔽列表
     */
    public void updatePlayerBlackList(long playerId, List<Long> playerBlackList) {
        if (playerBlackList.isEmpty()) {
            blackListRedisTemplate.opsForHash().delete(PLAYER_BLACK_LIST, playerId);
        } else {
            blackListRedisTemplate.opsForHash().put(PLAYER_BLACK_LIST, playerId, playerBlackList);
        }
    }
}
