package com.jjg.game.room.services;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.RedisLock;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
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
     * 通过游戏类型创建机器人,TODO 还可以通过添加机器人池的方式继续优化机器人的创建
     */
    public RobotPlayer getOrCreateRobotPlayer(int roomCfgId, long roomId) {
        String lockKey = robotDao.getLockRobotTableName();
        for (int i = 0; i < GameConstant.Redis.LOCK_TRY_TIMES; i++) {
            if (redisLock.tryLock(lockKey)) {
                try {
                    // 弹出一个可用的机器人ID
                    long randomRobotId = robotDao.popupOneRobotId();
                    RoomCfg roomCfg = GameDataManager.getRoomCfg(roomCfgId);
                    // 创建一个机器人
                    // 给机器人初始化数据，如果出现机器人某些数据找不到，在此处初始化
                    RobotPlayer robotPlayer = createRobot(randomRobotId, roomId, roomCfg);
                    // 将机器人写入当前服在用的机器人列表，机器人不用放数据库，后续有类似排行榜展示功能时需要单独处理
                    robotDao.recordCurServerRobotPlayer(robotPlayer);
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
    private RobotPlayer createRobot(long robotPlayerId, long roomId, RoomCfg roomCfg) {
        RobotPlayer robotPlayer = new RobotPlayer();
        robotPlayer.setId(robotPlayerId);
        RobotCfg robotCfg = GameDataManager.getRobotCfg((int) robotPlayerId);
        if (robotCfg == null) {
            // 此种情况应该是配置表删除了旧的机器人，但是数据中依旧还有旧的机器人ID，需要删除旧的机器人数据
            recycleRobotPlayer(robotPlayerId);
            log.error("配置表中找不到机器人ID：{}， 但在机器人池中还是存在此ID", robotPlayerId);
            return null;
        }
        long robotMoneyCarry = robotCfg.getMoney();
        List<List<Long>> addMoney = robotCfg.getAddMoney();
        Long randomGold = RandomUtils.randomLongMaxMinByWeightList(addMoney);
        if (randomGold == null) {
            log.error("机器人金币上下限配置异常");
            return null;
        }
        robotMoneyCarry += randomGold;
        robotPlayer.setRoomId(roomId);
        robotPlayer.setRoomCfgId(roomCfg.getId());
        robotPlayer.setNickName(robotCfg.getName());
        robotPlayer.setGold(robotMoneyCarry);
        robotPlayer.setGameType(roomCfg.getGameID());
        robotPlayer.setRoomCfgId(roomCfg.getId());
        return robotPlayer;
    }

    /**
     * 检查是否可以创建机器人
     */
    public boolean checkCanCreateRobot(int roomCfgId, Room room) {
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
        long curRobotNum = robotDao.getAvailableNum();
        if (curRobotNum == 0) {
            // 如果没有机器人可以创建,所有的机器人都已被分配完
            return false;
        }
        // 获取房间中机器人数量
        curRobotNum = room.countRobots();
        return curRobotNum < robotLimit;
    }

    /**
     * 删除机器人
     */
    public void recycleRobotPlayer(Long robotId) {
        String lockKey = robotDao.getLockRobotTableName();

        for (int i = 0; i < GameConstant.Redis.LOCK_TRY_TIMES; i++) {
            if (redisLock.tryLock(lockKey)) {
                try {
                    robotDao.recycleRobotPlayers(Collections.singleton(robotId));
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
    public void recycleRobotPlayers(List<Long> robotIds) {
        String lockKey = robotDao.getLockRobotTableName();
        for (int i = 0; i < GameConstant.Redis.LOCK_TRY_TIMES; i++) {
            if (redisLock.tryLock(lockKey)) {
                try {
                    robotDao.recycleRobotPlayers(robotIds);
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
        String lockKey = robotDao.getLockRobotTableName();
        // 这里需要多次尝试，一定需要确保删除成功
        for (int i = 0; i < GameConstant.Redis.LOCK_TRY_TIMES; i++) {
            if (redisLock.tryLock(lockKey)) {
                try {
                    //
                    Map<Long, String> allRobots = robotDao.getCurServerRobotPlayers();
                    if (allRobots == null || allRobots.isEmpty()) {
                        return;
                    }
                    String serverRobotTableName = robotDao.getCurServerRobotTableName();
                    // 删除数据
                    robotDao.recycleRobotPlayers(allRobots.keySet());
                    log.info("删除机器人Redis数据库：{} 中节点：{} 对应的机器人数据成功 删除数量：{}",
                        serverRobotTableName, nodePath, allRobots.size());
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
     * 机器人启动加载流程
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
    }

    /**
     * 初始化机器人ID库，预先将机器人数据写入机器人ID表中
     */
    public void checkOrInitRobotIdPool() {
        List<Long> robotCfgList =
            new ArrayList<>(GameDataManager.getRobotCfgList().stream().map(r -> (long) r.getId()).toList());
        String lockKey = robotDao.getLockRobotTableName();
        // 这里需要多次尝试，一定需要确保删除成功
        for (int i = 0; i < GameConstant.Redis.LOCK_TRY_TIMES; i++) {
            if (redisLock.tryLock(lockKey)) {
                try {
                    // 获取所有在使用的机器人表
                    List<Long> databaseAllRobot = robotDao.getAllUsedRobot();
                    // 移除所有数据库中的机器人ID
                    robotCfgList.removeAll(databaseAllRobot);
                    // 添加所有新增的机器人ID
                    robotDao.addNewRobotIds(robotCfgList);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                } finally {
                    redisLock.tryUnlock(lockKey);
                }
            }
        }
    }

    @Override
    public void shutdown() {
    }
}
