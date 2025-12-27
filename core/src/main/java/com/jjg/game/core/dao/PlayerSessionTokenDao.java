package com.jjg.game.core.dao;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.data.PlayerSessionToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/5/26 10:14
 */
@Repository
public class PlayerSessionTokenDao {
    private Logger log = LoggerFactory.getLogger(getClass());

    //玩家token
    private final String tableName = "playerSessionToken";
    //TODO
    //token过期时长
    private final long tokenExpireTime = 2400L * TimeHelper.ONE_HOUR_OF_MILLIS;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 保存token
     *
     * @param token
     * @param playerId
     */
    public void save(String token, int loginType, long playerId, int channel, String ip, int device, String mac, int registerChannel, String sharId) {
        PlayerSessionToken playerSessionToken = new PlayerSessionToken();
        playerSessionToken.setPlayerId(playerId);
        playerSessionToken.setToken(token);
        playerSessionToken.setExpireTime(System.currentTimeMillis() + tokenExpireTime);
        playerSessionToken.setLoginType(loginType);
        playerSessionToken.setChannel(channel);
        playerSessionToken.setIp(ip);
        playerSessionToken.setDevice(device);
        playerSessionToken.setMac(mac);
        playerSessionToken.setRegisterChannel(registerChannel);
        playerSessionToken.setSharId(sharId);
        save(playerSessionToken);
    }

    public void save(PlayerSessionToken playerSessionToken) {
        redisTemplate.opsForHash().put(tableName, playerSessionToken.getPlayerId(), playerSessionToken);
    }

    public void updateExpire(PlayerSessionToken playerSessionToken) {
        playerSessionToken.setExpireTime(System.currentTimeMillis() + tokenExpireTime);
        save(playerSessionToken);
    }

    /**
     * 获取db中session信息
     *
     * @param playerId
     * @return
     */
    public PlayerSessionToken getByPlayerId(long playerId) {
        return (PlayerSessionToken) redisTemplate.opsForHash().get(tableName, playerId);
    }

    public Long delTokens(List<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return 0l;
        }
        return redisTemplate.opsForHash().delete(tableName, playerIds.toArray());
    }

    public Long delToken(long playerId) {
        return redisTemplate.opsForHash().delete(tableName, playerId);
    }

    /**
     * 清除过期token
     */
    public int clearExpireToken() {
        Cursor<Map.Entry<Long, PlayerSessionToken>> cursor = null;
        int deleteCount = 0;
        try {
            cursor = redisTemplate.opsForHash().scan(
                    tableName,
                    ScanOptions.scanOptions().count(100).build());

            long now = System.currentTimeMillis();

            List<Long> delList = new ArrayList<>();
            while (cursor.hasNext()) {
                Map.Entry<Long, PlayerSessionToken> entry = cursor.next();

                PlayerSessionToken playerSessionToken = entry.getValue();
                if (playerSessionToken.getExpireTime() < now) {
                    delList.add(entry.getKey());
                }

                // 分批处理，避免内存占用过大
                if (delList.size() >= 50) {
                    deleteCount += delTokens(delList);
                    delList.clear();
                }
            }

            // 处理剩余未删除的字段
            if (!delList.isEmpty()) {
                deleteCount += delTokens(delList);
            }
        } catch (Exception e) {
            log.error("", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return deleteCount;
    }
}
