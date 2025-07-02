package com.jjg.game.room.dao;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.data.RobotPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisHashCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 机器人数据操作
 *
 * @author 2CL
 */
@Component
public class RobotDao {

    // 机器人redis数据 hash 键：robot+房间配置ID 数据：机器人ID <=> 机器人数据
    private static final String ROBOT_REDIS_KEY_PREFIX = "robot:";
    // 机器人ID redis数据 hash 键：robotId 数据：机器人ID <=> 当前机器人对应的redis key
    private static final String ROBOT_ID_REDIS_KEY_PREFIX = "robotId";

    private static final Logger log = LoggerFactory.getLogger(RobotDao.class);

    @Autowired
    private RedisTemplate<String, RobotPlayer> redisTemplate;

    public String getRobotTableName(int roomCfgId) {
        return ROBOT_REDIS_KEY_PREFIX + roomCfgId;
    }

    public String getLockRobotTableName(int roomCfgId) {
        return "lock:" + ROBOT_REDIS_KEY_PREFIX + roomCfgId;
    }

    /**
     * 获取机器人
     */
    public RobotPlayer getRobotPlayer(int roomCfgId, long robotId) {
        String robotKey = getRobotTableName(roomCfgId);
        return (RobotPlayer) redisTemplate.opsForHash().get(robotKey, robotId);
    }


    /**
     * 更新机器人数据
     */
    public void saveRobotPlayer(int roomCfgId, RobotPlayer robotPlayer) {
        String robotKey = getRobotTableName(roomCfgId);
        redisTemplate.opsForHash().put(robotKey, robotPlayer.getId(), robotPlayer);
    }

    /**
     * 获取当前已经创建的机器人ID,根据小房间进行划分
     */
    public Set<Integer> getRobotIdList(int roomCfgId) {
        return redisTemplate
            .opsForHash()
            .keys(getRobotTableName(roomCfgId))
            .stream()
            .map(robotObj -> (Integer) robotObj)
            .collect(Collectors.toSet());
    }

    /**
     * 创建一个机器人
     */
    public RobotPlayer createRobotPlayer(int roomCfgId, int robotId) {

        String robotKey = getRobotTableName(roomCfgId);
        RobotPlayer robotPlayer = new RobotPlayer();
        robotPlayer.setCreateTime(TimeHelper.nowInt());
        robotPlayer.setId(robotId);
        robotPlayer.setRoomCfgId(roomCfgId);

        List<Object> executedRes = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            Jackson2JsonRedisSerializer<RobotPlayer> jsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(RobotPlayer.class);
            RedisHashCommands hashCommands = connection.hashCommands();
            hashCommands.hSet(
                robotKey.getBytes(),
                (robotId + "").getBytes(),
                jsonRedisSerializer.serialize(robotPlayer)
            );
            hashCommands.hSet(
                ROBOT_ID_REDIS_KEY_PREFIX.getBytes(),
                (robotId + "").getBytes(),
                robotKey.getBytes()
            );
            return null;
        });
        boolean allSuccess = executedRes.stream().allMatch("OK"::equals);
        if (!allSuccess) {
            log.error("创建机器人，写入robot数据到redis中时失败");
        }
        return robotPlayer;
    }

    /**
     * 删除机器人
     */
    public void deleteRobotPlayer(int roomCfgId, int robotId) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisHashCommands hashCommands = connection.hashCommands();
            String robotKey = getRobotTableName(roomCfgId);
            byte[] robotBytes = (robotId + "").getBytes();
            hashCommands.hDel(robotKey.getBytes(), robotBytes);
            hashCommands.hDel(ROBOT_ID_REDIS_KEY_PREFIX.getBytes(), robotBytes);
            return null;
        });
    }

    /**
     * 获取当前机器人数量
     */
    public long getCurRobotNum(int roomCfgId) {
        return redisTemplate.opsForHash().size(getRobotTableName(roomCfgId));
    }
}
