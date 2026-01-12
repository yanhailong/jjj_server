package com.jjg.game.room.services;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 机器人处理逻辑，目前将机器人数据放在redis中，如果后续redis数据的IO过于频繁，考虑将机器人放入内存中管理
 *
 * @author lm
 */
@Service
public class RobotService implements IRoomStartListener, ConfigExcelChangeListener {

    private final Logger log = LoggerFactory.getLogger(RobotService.class);
    // 不同时间段房间创建机器人的人数限制
    private final Map<Integer, TreeMap<Integer, Integer>> roomRobotCreateLimit = new HashMap<>();
    //机器人缓存 金币数量,配置表id
    private TreeMap<Long, RobotCfg> robotCache = new TreeMap<>();
    //线程锁
    private final ReentrantLock lock = new ReentrantLock();
    private final RobotUtil robotUtil;
    private final MarsCurator marsCurator;

    public RobotService(RobotUtil robotUtil, MarsCurator marsCurator) {
        this.robotUtil = robotUtil;
        this.marsCurator = marsCurator;
    }


    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(RobotCfg.EXCEL_NAME, this::initRobotPool);
        addChangeSampleFileObserveWithCallBack(RobotCfg.EXCEL_NAME, this::initRobotPool);
    }

    /**
     * 获取机器人
     *
     * @param roomCfgId 房间配置id
     * @param roomId    房间id
     * @return 机器人
     */
    private RobotPlayer getRobotPlayer(int roomCfgId, long roomId) {
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        RoomCfg roomCfg = GameDataManager.getRoomCfg(roomCfgId);
        if (warehouseCfg == null || roomCfg == null) {
            return null;
        }
        long expend = 1;
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(111);
        if (globalConfigCfg != null) {
            expend = globalConfigCfg.getIntValue();
        }
        lock.lock();
        try {
            long enterLimit = warehouseCfg.getEnterLimit() * expend;
            NavigableMap<Long, RobotCfg> subbedMap = robotCache.subMap(enterLimit, true, Long.MAX_VALUE, true);
            if (subbedMap == null || subbedMap.isEmpty()) {
                return null;
            }
            for (Iterator<Map.Entry<Long, RobotCfg>> it = subbedMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, RobotCfg> entry = it.next();
                RobotCfg robotCfg = entry.getValue();
                Long gold = entry.getKey();
                //等级检查
                if (robotCfg.getPlayerLevel() < warehouseCfg.getPlayerLvLimit()) {
                    continue;
                }
                long checkNum = 0;
                if (warehouseCfg.getTransactionItemId() == ItemUtils.getGoldItemId()) {
                    checkNum = gold;
                }
                //货币检查
                if (checkNum < enterLimit || warehouseCfg.getEnterMax() != -1 && checkNum > warehouseCfg.getEnterMax()) {
                    continue;
                }
                it.remove();
                return createRobot(robotCfg, gold, roomId, roomCfg);
            }
        } catch (Exception e) {
            log.error("获取机器人异常 roomCfgId:{} roomId:{}", roomCfgId, roomId, e);
        } finally {
            lock.unlock();
        }
        return null;
    }


    /**
     * 创建robot playerController
     */
    public PlayerController getOrCreateRobotPlayerController(int roomCfgId, long roomId) {
        RobotPlayer robotPlayer = getRobotPlayer(roomCfgId, roomId);
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
    private RobotPlayer createRobot(RobotCfg robotCfg, long gold, long roomId, RoomCfg roomCfg) {
        RobotPlayer robotPlayer = robotUtil.initRobotPlayer(robotCfg);
        robotPlayer.setRoomId(roomId);
        robotPlayer.setRoomCfgId(roomCfg.getId());
        robotPlayer.setGold(gold);
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
        long curRobotNum = robotCache.size();
        if (curRobotNum == 0) {
            // 如果没有机器人可以创建,所有的机器人都已被分配完
            return false;
        }
        // 获取房间中机器人数量
        curRobotNum = room.countRobots();
        return curRobotNum < robotLimit;
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
    }


    private long getRobotRealMoney(RobotCfg cfg) {
        return RandomUtils.randomWeightList(cfg.getAddMoney());
    }

    /**
     * 缓存机器人信息
     */
    public void initRobotPool() {
        //机器人id 机器人最少金币
        TreeMap<Long, RobotCfg> tempRobotCache = new TreeMap<>();
        List<RobotCfg> robotCfgList = GameDataManager.getRobotCfgList();
        for (RobotCfg robotCfg : robotCfgList) {
            if (robotCfg.getAvailable() != 0) {
                continue;
            }
            long robotRealMoney = getRobotRealMoney(robotCfg);
            tempRobotCache.put(robotRealMoney, robotCfg);
        }
        robotCache = tempRobotCache;
    }

    /**
     * 回收机器人
     */
    public void recycleRobotPlayers(List<Long> robotIds) {
        //重新放入
        long robotStartId = robotUtil.getRobotStartId();
        lock.lock();
        try {
            for (Long robotId : robotIds) {
                int configId = (int) (robotId - robotStartId) / GameConstant.ROBOT_ID_PRIME_NUMBER;
                RobotCfg robotCfg = GameDataManager.getRobotCfg(configId);
                if (robotCfg == null) {
                    continue;
                }
                long robotRealMoney = getRobotRealMoney(robotCfg);
                robotCache.put(robotRealMoney, robotCfg);
            }
        } catch (Exception e) {
            log.error("recycleRobotPlayer error robotId:{}", robotIds, e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 回收机器人
     */
    public void recycleRobotPlayer(long robotId) {
        //重新放入
        long robotStartId = robotUtil.getRobotStartId();
        int configId = (int) (robotId - robotStartId) / GameConstant.ROBOT_ID_PRIME_NUMBER;
        RobotCfg robotCfg = GameDataManager.getRobotCfg(configId);
        if (robotCfg == null) {
            return;
        }
        lock.lock();
        try {
            long robotRealMoney = getRobotRealMoney(robotCfg);
            robotCache.put(robotRealMoney, robotCfg);
        } catch (Exception e) {
            log.error("recycleRobotPlayer error robotId:{}", robotId, e);
        } finally {
            lock.unlock();
        }
    }

    public RobotCfg getRobotCfg(long robotId) {
        return robotUtil.getRobotCfg(robotId);
    }

    @Override
    public void shutdown() {
    }

}
