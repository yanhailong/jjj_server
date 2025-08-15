package com.jjg.game.room.dao;

import com.jjg.game.common.constant.StrConstant;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.core.data.RobotPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

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

    // 机器人redis数据 set 键：robotIdList+房间配置ID 数据：机器人ID <=> 机器人数据
    private static final String ROBOT_ID_LIST_REDIS_KEY_PREFIX = "RobotIdList" + StrConstant.COLON;
    // 每个节点的所有机器人 键：servers_robot+节点路径 数据：机器人ID <=> 机器人redis数据
    private static final String SERVER_OF_ROBOT = "ClusterRobot";

    private static final Logger log = LoggerFactory.getLogger(RobotDao.class);

    @Autowired
    private RedisTemplate<String, Long> redisTemplate;
    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;
    @Autowired
    private NodeManager nodeManager;

    public String getRobotIdListTableName() {
        return ROBOT_ID_LIST_REDIS_KEY_PREFIX;
    }

    /**
     * 获取存放当前服的机器人key
     */
    public String getCurServerRobotTableName() {
        String nodePath = nodeManager.getNodePath();
        String nodePathDataKey = nodePath.replace(StrConstant.SLASH, StrConstant.COLON);
        return SERVER_OF_ROBOT + nodePathDataKey;
    }

    public String getLockRobotTableName() {
        return "lock" + StrConstant.COLON + ROBOT_ID_LIST_REDIS_KEY_PREFIX;
    }

    /**
     * 更新机器人数据
     */
    public void recordCurServerRobotPlayer(RobotPlayer robotPlayer) {
        long robotId = robotPlayer.getId();
        String serverRobotTableName = getCurServerRobotTableName();
        stringRedisTemplate.opsForHash().put(serverRobotTableName, robotId + "", System.currentTimeMillis() + "");
    }

    /**
     * 弹出一个机器人Id
     */
    public long popupOneRobotId() {
        String robotId = redisTemplate.opsForSet().pop(getRobotIdListTableName()) + "";
        return Long.parseLong(robotId);
    }

    /**
     * 删除机器人
     */
    public void recycleRobotPlayers(Collection<Long> robotIds) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            recycleServerRobotByTableKey(connection, robotIds);
            return null;
        });
    }

    /**
     * 删除tableKey中指定ID的机器人
     */
    private void recycleServerRobotByTableKey(RedisConnection connection, Collection<Long> robotIds) {
        byte[][] robotBytes = new byte[robotIds.size()][];
        Long[] robotIdsArray = robotIds.toArray(new Long[0]);
        for (int i = 0; i < robotIdsArray.length; i++) {
            robotBytes[i] = (robotIdsArray[i] + "").getBytes();
        }
        String robotKey = getRobotIdListTableName();
        String serverRobotTableName = getCurServerRobotTableName();
        // 向总的机器人池重新加入机器人ID数据
        connection.setCommands().sAdd(robotKey.getBytes(), robotBytes);
        // 删除服务器中的ID
        connection.hashCommands().hDel(serverRobotTableName.getBytes(), robotBytes);
    }

    /**
     * 获取当前服的所有的机器人
     */
    public Map<Long, String> getCurServerRobotPlayers() {
        String serverRobotTableName = getCurServerRobotTableName();
        return stringRedisTemplate.opsForHash().entries(serverRobotTableName)
            .entrySet()
            .stream()
            .collect(HashMap::new,
                (map, e)
                    -> map.put(Long.valueOf((String) e.getKey()), (String) (e.getValue())),
                HashMap::putAll);
    }

    /**
     * 获取当前总池子里面可用的机器人数量
     */
    public long getAvailableNum() {
        Long size = redisTemplate.opsForSet().size(getRobotIdListTableName());
        return size == null ? 0 : size;
    }

    /**
     * 获取redis中所有的机器人ID数据，仅服务器启动加载时调用
     */
    public List<Long> getAllUsedRobot() {
        String redisIdListKey = getRobotIdListTableName();
        // 未被使用的机器人ID列表
        Set<String> allRobotStrIdList = stringRedisTemplate.opsForSet().members(redisIdListKey);
        if (allRobotStrIdList == null || allRobotStrIdList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> allRobotIdList = new ArrayList<>(allRobotStrIdList.stream().map(Long::parseLong).toList());
        // 获取各个服正在使用的机器人
        Set<String> serverUsedRobotKeys = redisTemplate.keys(SERVER_OF_ROBOT + StrConstant.ASTERISK);
        if (serverUsedRobotKeys.isEmpty()) {
            return allRobotIdList;
        }
        Set<Long> serversUsedIdList =
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (String serverUsedRobotKey : serverUsedRobotKeys) {
                        connection.hashCommands().hKeys(serverUsedRobotKey.getBytes());
                    }
                    return null;
                }).stream()
                .map(a -> (Set<String>) a)
                .map(a -> a.stream().map(Long::parseLong).toList())
                .collect(HashSet::new, HashSet::addAll, HashSet::addAll);
        log.info("可用的机器人数量：{} 服务节点已用的数量：{}", allRobotIdList.size(), serversUsedIdList.size());
        allRobotIdList.addAll(serversUsedIdList);
        return allRobotIdList;
    }

    /**
     * 添加新的机器人ID
     */
    public void addNewRobotIds(List<Long> newRobotIdList) {
        String redisIdListKey = getRobotIdListTableName();
        stringRedisTemplate.opsForSet()
            .add(redisIdListKey, newRobotIdList.stream().map(String::valueOf).toArray(String[]::new));
    }
}
