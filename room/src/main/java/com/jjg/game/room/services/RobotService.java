package com.jjg.game.room.services;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.RedisLock;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.dao.RobotDao;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.RobotCfg;
import com.jjg.game.room.sample.bean.RoomCfg;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 机器人处理逻辑，目前将机器人数据放在redis中，如果后续redis数据的IO过于频繁，考虑将机器人放入内存中管理
 *
 * @author 2CL
 */
@Service
public class RobotService implements IRoomStartListener {

    private static final Logger log = LoggerFactory.getLogger(RobotService.class);
    // 不同时间段房间创建机器人的人数限制
    private final Map<Integer, TreeMap<Integer, Integer>> roomRobotCreateLimit = new HashMap<>();

    @Autowired
    private RobotDao robotDao;
    @Autowired
    private RedisLock redisLock;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private MarsCurator marsCurator;

    /**
     * 通过游戏类型创建机器人
     * TODO 后续优化，先临时创建一个机器人，不存库，等外部逻辑判断完成，最后入库
     */
    public RobotPlayer getOrCreateRobotPlayer(int roomCfgId, long roomId) {
        String lockKey = robotDao.getLockRobotTableName(roomCfgId + "");
        for (int i = 0; i < GameConstant.Redis.LOCK_TRY_TIMES; i++) {
            if (redisLock.tryLock(lockKey)) {
                try {
                    if (!checkCanCreateRobot(roomCfgId)) {
                        return null;
                    }
                    // 已经创建的机器人ID列表
                    Set<Integer> robotIdList = robotDao.getRobotIdList(roomCfgId);
                    // 机器人ID
                    List<Integer> configuredRobotIdList =
                        GameDataManager.getRobotCfgList()
                            .stream()
                            // 为0时可用
                            .filter(cfg -> cfg.getAvailable() == 0)
                            .map(RobotCfg::getId)
                            .distinct()
                            .collect(Collectors.toList());
                    configuredRobotIdList.removeAll(robotIdList);
                    // 如果机器人已经达到创建上限,则停止创建
                    if (configuredRobotIdList.isEmpty()) {
                        log.warn("机器人已使用完");
                        return null;
                    }
                    Integer robotId = RandomUtils.randCollection(configuredRobotIdList);
                    if (robotId == null) {
                        return null;
                    }
                    RobotCfg robotCfg = GameDataManager.getRobotCfg(robotId);
                    // 创建一个机器人
                    RobotPlayer robotPlayer = robotDao.createRobotPlayer(roomCfgId, robotId);
                    RoomCfg roomCfg = GameDataManager.getRoomCfg(roomCfgId);
                    robotPlayer.setRoomId(roomId);
                    robotPlayer.setRoomCfgId(roomCfgId);
                    robotPlayer.setNickName(robotCfg.getName());
                    // 给机器人初始化数据，如果出现机器人某些数据找不到，在此处初始化
                    initialRobotData(robotPlayer, roomCfg);
                    // 回存一次数据 TODO 后续修改
                    robotDao.saveRobotPlayer(roomCfg.getId(), robotPlayer);
                    return robotPlayer;
                } finally {
                    redisLock.tryUnlock(lockKey);
                }
            }
            try {
                Thread.sleep(GameConstant.Redis.PER_TRY_TAKE_MILE_TIME);
            } catch (InterruptedException ignored) {
            }
        }
        return null;
    }

    /**
     * 创建robot playerController
     * TODO 后续优化，先临时创建一个机器人，不存库，等外部逻辑判断完成，最后入库
     */
    public PlayerController getOrCreateRobotPlayerController(int roomCfgId, long roomId) {
        RobotPlayer robotPlayer = getOrCreateRobotPlayer(roomCfgId, roomId);
        if (robotPlayer == null) {
            return null;
        }
        String nodePath = marsCurator.nodePath;
        PFSession robotSession = new PFSession(null, null, null);
        robotSession.setGatePath(nodePath);
        return new PlayerController(robotSession, robotPlayer);
    }

    /**
     * 初始化机器人数据
     */
    private void initialRobotData(RobotPlayer robotPlayer, RoomCfg roomCfg) {
        long robotPlayerId = robotPlayer.getId();
        RobotCfg robotCfg = GameDataManager.getRobotCfg((int) robotPlayerId);
        long robotMoneyCarry = robotCfg.getMoney();
        List<List<Integer>> addMoney = robotCfg.getAddMoney();
        Integer randomGold = RandomUtils.randomMaxMinByWeightList(addMoney);
        if (randomGold == null) {
            log.error("机器人金币上下限配置异常");
            return;
        }
        robotMoneyCarry += randomGold;
        robotPlayer.setGold(robotMoneyCarry);
        robotPlayer.setGameType(roomCfg.getGameID());
        robotPlayer.setRoomCfgId(roomCfg.getId());
    }

    /**
     * 检查是否可以创建机器人
     */
    private boolean checkCanCreateRobot(int roomCfgId) {
        // 检查当前游戏的人数是否达到上限
        TreeMap<Integer, Integer> createLimitByTime = roomRobotCreateLimit.get(roomCfgId);
        Calendar calendar = DateUtils.toCalendar(new Date(System.currentTimeMillis()));
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int robotLimit = 0;
        for (Map.Entry<Integer, Integer> entry : createLimitByTime.descendingMap().entrySet()) {
            if (hour <= entry.getKey()) {
                robotLimit = entry.getValue();
            } else {
                break;
            }
        }
        // 获取当前机器人数量
        long curRobotNum = robotDao.getCurRobotNum(roomCfgId);
        return curRobotNum < robotLimit;
    }

    /**
     * 删除机器人
     */
    public void deleteRobotPlayer(int roomCfgId, Long robotId) {
        String lockKey = robotDao.getLockRobotTableName(roomCfgId + "");

        for (int i = 0; i < GameConstant.Redis.LOCK_TRY_TIMES; i++) {
            if (redisLock.tryLock(lockKey)) {
                try {
                    robotDao.deleteRobotPlayers(roomCfgId, Collections.singleton(robotId));
                } finally {
                    redisLock.tryUnlock(lockKey);
                }
            }
            try {
                Thread.sleep(GameConstant.Redis.PER_TRY_TAKE_MILE_TIME);
            } catch (InterruptedException ignored) {
            }
        }
    }


    /**
     * 删除机器人
     */
    public void deleteRobotPlayers(int roomCfgId, List<Long> robotIds) {
        String lockKey = robotDao.getLockRobotTableName(roomCfgId + "");
        for (int i = 0; i < GameConstant.Redis.LOCK_TRY_TIMES; i++) {
            if (redisLock.tryLock(lockKey)) {
                try {
                    robotDao.deleteRobotPlayers(roomCfgId, robotIds);
                } finally {
                    redisLock.tryUnlock(lockKey);
                }
            }
            try {
                Thread.sleep(GameConstant.Redis.PER_TRY_TAKE_MILE_TIME);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * 当服务器关闭时，删除当前服的所有机器人
     */
    public void deleteServerAllRobot() {
        String nodePath = nodeManager.getNodePath();
        String lockKey = robotDao.getLockRobotTableName(nodePath);
        // 这里需要多次尝试，一定需要确保删除成功
        for (int i = 0; i < GameConstant.Redis.LOCK_TRY_TIMES; i++) {
            if (redisLock.tryLock(lockKey)) {
                try {
                    Map<Long, String> allRobots = robotDao.getCurServerRobotPlayers();
                    if (allRobots == null || allRobots.isEmpty()) {
                        return;
                    }
                    Map<String, List<Long>> robotTableKeyOfRobots = new HashMap<>();
                    for (Map.Entry<Long, String> entry : allRobots.entrySet()) {
                        robotTableKeyOfRobots.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
                    }
                    // 删除redis中的机器人数据
                    for (Map.Entry<String, List<Long>> entry : robotTableKeyOfRobots.entrySet()) {
                        // 删除数据
                        robotDao.deleteRobotPlayers(entry.getKey(), entry.getValue());
                        log.info("删除机器人Redis数据库：{} 中节点：{} 对应的机器人数据成功 删除数量：{}",
                            entry.getKey(), nodePath, entry.getValue().size());
                    }
                    break;
                } finally {
                    redisLock.tryUnlock(lockKey);
                }
            }
            try {
                Thread.sleep(GameConstant.Redis.PER_TRY_TAKE_MILE_TIME);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     *
     */
    @Override
    public void start() {
        // 检查机器人数量是否配置正确,robot表中的机器人创建限制字段和机器人表中对应游戏的机器人数量是否一致
        List<RoomCfg> roomCfgList = GameDataManager.getRoomCfgList();
        for (RoomCfg roomCfg : roomCfgList) {
            List<List<Integer>> robotNumList = roomCfg.getRobot_num();
            for (List<Integer> robotNumConf : robotNumList) {
                roomRobotCreateLimit
                    .computeIfAbsent(roomCfg.getId(), k -> new TreeMap<>())
                    .put(robotNumConf.get(1), robotNumConf.get(2));
            }
        }
        // TODO 如果是开服就创建房间的游戏，检查机器人填充上限是否大于房间人数上限，如果出现这种情况会出现房间全是机器人的情况

        // TODO 定时检查是否有异常的房间机器人数据
    }

    @Override
    public void shutdown() {

    }
}
