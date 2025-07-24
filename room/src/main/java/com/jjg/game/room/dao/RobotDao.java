package com.jjg.game.room.dao;

import com.jjg.game.common.constant.StrConstant;
import com.jjg.game.common.curator.NodeManager;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * 机器人数据操作
 * <p>
 * 机器的创建时机：
 * <br>1.房间初始化时，需要通过创建机器人来开房间
 * <br>2.在房间中途加入，如要模拟玩家操作行为时，会有概率创建机器人
 * 机器人销毁时机:
 * <br>1.服务器重启或者关服时，需要将当前服的所有房间的机器人移除
 * <br>2.游戏结算时有概率退出，从数据中移除
 *
 * @author 2CL
 */
@Component
public class RobotDao {

    // 机器人redis数据 hash 键：robot+房间配置ID 数据：机器人ID <=> 机器人数据
    private static final String ROBOT_REDIS_KEY_PREFIX = "robot:";
    // 每个节点的所有机器人
    private static final String SERVER_OF_ROBOT = "servers_robot:";

    private static final Logger log = LoggerFactory.getLogger(RobotDao.class);

    @Autowired
    private RedisTemplate<String, RobotPlayer> redisTemplate;
    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;
    @Autowired
    private NodeManager nodeManager;

    public String getRobotTableName(int roomCfgId) {
        return ROBOT_REDIS_KEY_PREFIX + roomCfgId;
    }

    /**
     * 获取存放当前服的机器人key
     */
    public String getCurServerRobotTableName() {
        String nodePath = nodeManager.getNodePath();
        String nodePathDataKey = nodePath.replace(StrConstant.SLASH, StrConstant.COLON);
        return SERVER_OF_ROBOT + StrConstant.COLON + nodePathDataKey;
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
        String serverRobotTableName = getCurServerRobotTableName();
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
                serverRobotTableName.getBytes(),
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
    public void deleteRobotPlayer(int roomCfgId, Collection<Long> robotIds) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisHashCommands hashCommands = connection.hashCommands();
            String robotKey = getRobotTableName(roomCfgId);
            byte[][] robotBytes = new byte[robotIds.size()][];
            Long[] robotIdsArray = (Long[]) robotIds.toArray();
            for (int i = 0; i < robotIdsArray.length; i++) {
                robotBytes[i] = (robotIdsArray[i] + "").getBytes();
            }
            hashCommands.hDel(robotKey.getBytes(), robotBytes);
            String serverRobotTableName = getCurServerRobotTableName();
            hashCommands.hDel(serverRobotTableName.getBytes(), robotBytes);
            return null;
        });
    }

    /**
     * 获取当前服的所有的机器人
     */
    public Map<String, String> getAllServerRobotPlayers() {
        String serverRobotTableName = getCurServerRobotTableName();
        return stringRedisTemplate.opsForHash().entries(serverRobotTableName)
            .entrySet()
            .stream()
            .collect(HashMap::new, (map, e) -> map.put((String) e.getKey(), (String) e.getValue()), HashMap::putAll);
    }

    /**
     * 获取当前机器人数量
     */
    public long getCurRobotNum(int roomCfgId) {
        return redisTemplate.opsForHash().size(getRobotTableName(roomCfgId));
    }
}
