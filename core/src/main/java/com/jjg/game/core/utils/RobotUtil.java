package com.jjg.game.core.utils;

import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RobotCfg;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/11/24 15:50
 */
@Component
public class RobotUtil {
    private final NodeConfig nodeConfig;

    public RobotUtil(NodeConfig nodeConfig) {
        this.nodeConfig = nodeConfig;
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
        long robotStartId = getRobotStartId();
        robotPlayer.setId(robotStartId + (long) GameConstant.ROBOT_ID_PRIME_NUMBER * robotCfg.getId());
        robotPlayer.setLevel(robotCfg.getPlayerLevel());
        robotPlayer.setVipLevel(robotCfg.getVipLevel());
        robotPlayer.setGender((byte) robotCfg.getGender());
        robotPlayer.setHeadFrameId(robotCfg.getFrame());
        robotPlayer.setNationalId(robotCfg.getFlag());
        robotPlayer.setHeadImgId(robotCfg.getPicture());
        robotPlayer.setNickName("player" + robotPlayer.getId());
        return robotPlayer;
    }

    public long getRobotStartId() {
        long robotStartId = nodeConfig.getRobotStartId();
        if (robotStartId == 0) {
            robotStartId = 1000000;
        }
        return robotStartId;
    }

    public RobotCfg getRobotCfg(long robotId) {
        long robotStartId = getRobotStartId();
        int configId = (int) (robotId - robotStartId) / GameConstant.ROBOT_ID_PRIME_NUMBER;
        return GameDataManager.getRobotCfg(configId);
    }
}
