package com.jjg.game.room.dao;

import com.jjg.game.common.constant.StrConstant;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.core.data.RobotPlayer;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final String ROBOT_ID_LIST_REDIS_KEY_PREFIX = "RobotIdSet" + StrConstant.COLON;
    // 每个节点的所有机器人 键：servers_robot+节点路径 数据：机器人ID <=> 机器人redis数据
    private final String SERVER_OF_ROBOT = "ClusterRobotId";


    @Autowired
    private RedissonClient redissonClient;
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
        redissonClient.getMap(serverRobotTableName).put(robotId, System.currentTimeMillis() + "");
        RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(getRobotIdListTableName());
        scoredSortedSet.add(0, robotId);
    }

    /**
     * 获取金币大于参数的机器人id
     */
    public List<ScoredEntry<Long>> getCanUseRobotIds(double gold, int startIndex, int size) {
        RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(getRobotIdListTableName());
        return new ArrayList<>(scoredSortedSet.entryRange(gold, true, Double.MAX_VALUE, true, startIndex, size));
    }

    /**
     * 删除机器人
     */
    public void recycleRobotPlayers(Map<Long, Double> robotIds) {
        //删除记录
        String serverRobotTableName = getCurServerRobotTableName();
        int batchSize = 500;
        List<Long> ids = new ArrayList<>(robotIds.keySet());
        for (int i = 0; i < ids.size(); i += batchSize) {
            int end = Math.min(i + batchSize, ids.size());
            redissonClient.getMap(serverRobotTableName)
                    .fastRemove(ids.subList(i, end).toArray(new Long[0]));
        }
        //添加分数
        addNewRobotIds(robotIds);
    }


    /**
     * 获取当前服的所有的机器人
     */
    public Set<Long> getCurServerRobotPlayers() {
        String serverRobotTableName = getCurServerRobotTableName();
        Map<Long, String> rMap = redissonClient.getMap(serverRobotTableName);
        return rMap.keySet();
    }

    /**
     * 获取当前总池子里面可用的机器人数量
     */
    public long getAvailableNum() {
        RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(getRobotIdListTableName());
        Collection<Long> available = scoredSortedSet.valueRange(0, false, Double.MAX_VALUE, true);
        return available == null ? 0 : available.size();
    }

    /**
     * 获取redis中所有的机器人ID数据，仅服务器启动加载时调用
     */
    public List<Long> getAllUsedRobot() {
        RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(getRobotIdListTableName());
        return new ArrayList<>(scoredSortedSet.readAll());
    }

    /**
     * 添加新的机器人ID
     */
    public void addNewRobotIds(Map<Long, Double> newRobotIdList) {
        RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(getRobotIdListTableName());
        scoredSortedSet.addAll(newRobotIdList);
    }
}
