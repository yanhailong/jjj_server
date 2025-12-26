package com.jjg.game.core.utils;

import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RobotCfg;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/11/24 15:50
 */
@Component
public class RobotUtil {
    private final NodeConfig nodeConfig;
    private final RedissonClient redissonClient;
    //每个节点的计数
    private final String NODE_KEY = "node:robot";
    //每个节点的起始id
    private final String NODE_ID_KEY = "node:robot:id:%s";
    //全局起始id
    private final String NODE_START_KEY = "node:robot:start";
    private static long ROBOT_START_NODE_ID = 0;
    private static long ROBOT_START_ID = 0;

    public RobotUtil(NodeConfig nodeConfig, RedissonClient redissonClient) {
        this.nodeConfig = nodeConfig;
        this.redissonClient = redissonClient;
    }

    public RobotPlayer randomRobotPlayer() {
        RobotCfg robotCfg = RandomUtil.randomEle(GameDataManager.getRobotCfgList());
        return initRobotPlayer(robotCfg);
    }

    /**
     * 初始化机器人数据
     */
    public RobotPlayer initRobotPlayer(RobotCfg robotCfg) {
        if (robotCfg == null) {
            return null;
        }
        RobotPlayer robotPlayer = new RobotPlayer();
        //计算id

        robotPlayer.setId(getId(robotCfg.getId()));
        robotPlayer.setLevel(robotCfg.getPlayerLevel());
        robotPlayer.setVipLevel(robotCfg.getVipLevel());
        robotPlayer.setGender((byte) robotCfg.getGender());
        robotPlayer.setHeadFrameId(robotCfg.getFrame());
        robotPlayer.setNationalId(robotCfg.getFlag());
        robotPlayer.setHeadImgId(robotCfg.getPicture());
        robotPlayer.setNickName("player" + robotPlayer.getId());
        return robotPlayer;
    }

    public long getId(long robotId) {
        long robotStartId = getRobotStartId();
        return robotStartId + GameConstant.ROBOT_ID_PRIME_NUMBER * robotId;
    }


    public long getRobotStartId() {
        //获取节点 ID
        if (ROBOT_START_NODE_ID == 0) {
            RAtomicLong bucket = redissonClient.getAtomicLong(NODE_ID_KEY.formatted(nodeConfig.getName()));
            long id = bucket.get();
            if (id == 0) {
                RAtomicLong nodeId = redissonClient.getAtomicLong(NODE_KEY);
                id = nodeId.incrementAndGet();
                bucket.compareAndSet(0, id);
            }
            ROBOT_START_NODE_ID = id;
        }
        //获取起始 ID
        if (ROBOT_START_ID == 0) {
            RAtomicLong bucket = redissonClient.getAtomicLong(NODE_START_KEY);
            long baseStartId = bucket.get();
            if (baseStartId == 0) {
                baseStartId = 1000000L;
            }
            ROBOT_START_ID = getRobotStartId(baseStartId + (ROBOT_START_NODE_ID - 1) * GameDataManager.getRobotCfgList().size() * GameConstant.ROBOT_ID_PRIME_NUMBER);
        }
        return ROBOT_START_ID;
    }

    public long getRobotStartId(long baseStartId) {
        for (int i = 1; i <= GameConstant.ROBOT_ID_PRIME_NUMBER + 1; i++) {
            long startId = baseStartId + i;
            if (startId % GameConstant.ROBOT_ID_PRIME_NUMBER == 0) {
                return startId;
            }
        }
        return baseStartId;
    }

    /**
     * 初始化全局机器Id
     *
     * @param startId 起始账号ID
     */
    public void initRobotStartId(long startId) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(NODE_START_KEY);
        atomicLong.compareAndSet(0, startId);
    }

    public RobotCfg getRobotCfg(long robotId) {
        long robotStartId = getRobotStartId();
        int configId = (int) (robotId - robotStartId) / GameConstant.ROBOT_ID_PRIME_NUMBER;
        return GameDataManager.getRobotCfg(configId);
    }

    public boolean isRobot(long robotId) {
        return robotId % GameConstant.ROBOT_ID_PRIME_NUMBER == 0;
    }

}


