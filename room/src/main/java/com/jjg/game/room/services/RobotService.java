package com.jjg.game.room.services;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.room.dao.RobotDao;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RobotCfg;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.apache.commons.lang3.time.DateUtils;
import org.redisson.client.protocol.ScoredEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 机器人处理逻辑，目前将机器人数据放在redis中，如果后续redis数据的IO过于频繁，考虑将机器人放入内存中管理
 *
 * @author 2CL
 */
@Service
public class RobotService implements IRoomStartListener {

    private final Logger log = LoggerFactory.getLogger(RobotService.class);
    // 不同时间段房间创建机器人的人数限制
    private final Map<Integer, TreeMap<Integer, Integer>> roomRobotCreateLimit = new HashMap<>();

    private final RobotDao robotDao;
    private final RedisLock redisLock;
    private final NodeManager nodeManager;
    private final MarsCurator marsCurator;
    private final int MAX_WAIT_TIME =
            GameConstant.Redis.LOCK_TRY_TIMES * GameConstant.Redis.PER_TRY_TAKE_MILE_TIME;

    public RobotService(RobotDao robotDao, RedisLock redisLock, NodeManager nodeManager, MarsCurator marsCurator) {
        this.robotDao = robotDao;
        this.redisLock = redisLock;
        this.nodeManager = nodeManager;
        this.marsCurator = marsCurator;
    }

    /**
     * 通过游戏类型一定创建机器人,TODO 还可以通过添加机器人池的方式继续优化机器人的创建
     */
    public RobotPlayer getOrCreateRobotPlayerForce(int roomCfgId, long roomId) {
        String lockKey = robotDao.getLockRobotTableName();
        boolean locked = false;
        try {
            locked = redisLock.tryLock(lockKey, MAX_WAIT_TIME);
            if (locked) {
                return getRobotPlayer(roomCfgId, roomId);
            }
        } catch (InterruptedException ignored) {
        } finally {
            if (locked) {
                redisLock.tryUnlock(lockKey);
            }
        }
        return null;
    }

    /**
     * 通过游戏类型创建机器人
     */
    public RobotPlayer getOrCreateRobotPlayer(int roomCfgId, long roomId) {
        String lockKey = robotDao.getLockRobotTableName();
        if (redisLock.tryLock(lockKey)) {
            try {
                return getRobotPlayer(roomCfgId, roomId);
            } catch (Exception e) {
                log.error("获取机器人异常", e);
            } finally {
                redisLock.unlock(lockKey);
            }
        }
        return null;
    }

    /**
     * 获取机器人
     *
     * @param roomCfgId 房间配置id
     * @param roomId    房间id
     * @return 机器人
     */
    private RobotPlayer getRobotPlayer(int roomCfgId, long roomId) {
        int size = 5;
        RoomCfg roomCfg = GameDataManager.getRoomCfg(roomCfgId);
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        long maxGetCount = (robotDao.getAvailableNum() / size) + 1;
        //生成最低分数 取出满足最低金币进入限制的机器人id
        double finalScore = Math.max(1, robotDao.getFinalScore(Pair.newPair(warehouseCfg.getPlayerLvLimit(), (long) warehouseCfg.getEnterLimit())));
        for (int i = 0; i < maxGetCount; i++) {
            List<ScoredEntry<Long>> canUseRobotIds = robotDao.getCanUseRobotIds(finalScore, i * size, size);
            //没有比限制大的直接返回
            if (canUseRobotIds.isEmpty()) {
                return null;
            }
            if (i > 1) {
                log.warn("多次寻找机器人 需要分数{} 需要等级{} 金币{} 第一个分数{}", finalScore, warehouseCfg.getPlayerLvLimit(), warehouseCfg.getEnterLimit()
                        , canUseRobotIds.getFirst().getScore());
            }
            Collections.shuffle(canUseRobotIds);
            for (ScoredEntry<Long> robotId : canUseRobotIds) {
                // 创建一个机器人
                // 给机器人初始化数据，如果出现机器人某些数据找不到，在此处初始化
                RobotPlayer robotPlayer = createRobot(robotId, roomId, roomCfg);
                if (robotPlayer == null) {
                    continue;
                }
                //等级检查
                if (robotPlayer.getLevel() < warehouseCfg.getPlayerLvLimit()) {
                    continue;
                }
                long checkNum = 0;
                if (warehouseCfg.getTransactionItemId() == ItemUtils.getGoldItemId()) {
                    checkNum = robotPlayer.getGold();
                }
                if (warehouseCfg.getTransactionItemId() == ItemUtils.getDiamondItemId()) {
                    checkNum = robotPlayer.getDiamond();
                }
                //货币检查
                if (checkNum < warehouseCfg.getEnterLimit() || warehouseCfg.getEnterMax() != -1 && checkNum > warehouseCfg.getEnterMax()) {
                    continue;
                }
                // 将机器人写入当前服在用的机器人列表，机器人不用放数据库，后续有类似排行榜展示功能时需要单独处理
                robotDao.recordCurServerRobotPlayer(robotPlayer);
                return robotPlayer;
            }
        }
        return null;
    }


    /**
     * 创建robot playerController
     */
    public PlayerController getOrCreateRobotPlayerController(int roomCfgId, long roomId) {
        return getOrCreateRobotPlayerController(roomCfgId, roomId, false);
    }

    /**
     * 创建robot playerController
     */
    public PlayerController getOrCreateRobotPlayerController(int roomCfgId, long roomId, boolean force) {
        RobotPlayer robotPlayer;
        if (force) {
            robotPlayer = getOrCreateRobotPlayerForce(roomCfgId, roomId);
        } else {
            robotPlayer = getOrCreateRobotPlayer(roomCfgId, roomId);
        }
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
    private RobotPlayer createRobot(ScoredEntry<Long> robotParam, long roomId, RoomCfg roomCfg) {
        RobotPlayer robotPlayer = new RobotPlayer();
        Long robotPlayerId = robotParam.getValue();
        robotPlayer.setId(robotPlayerId);
        RobotCfg robotCfg = GameDataManager.getRobotCfg((int) robotPlayer.getId());
        if (robotCfg == null) {
            // 此种情况应该是配置表删除了旧的机器人，但是数据中依旧还有旧的机器人ID，需要删除旧的机器人数据
            recycleRobotPlayer(robotPlayerId);
            log.error("配置表中找不到机器人ID：{}， 但在机器人池中还是存在此ID", robotPlayerId);
            return null;
        }
        robotPlayer.setLevel(robotCfg.getPlayerLevel());
        robotPlayer.setVipLevel(robotCfg.getVipLevel());
        robotPlayer.setGender((byte) robotCfg.getGender());
        robotPlayer.setHeadFrameId(robotCfg.getFrame());
        robotPlayer.setNationalId(robotCfg.getFlag());
        robotPlayer.setHeadImgId(robotCfg.getPicture());
        robotPlayer.setRoomId(roomId);
        robotPlayer.setRoomCfgId(roomCfg.getId());
        robotPlayer.setNickName(robotCfg.getName());
        robotPlayer.setGold(robotDao.parseGold(robotParam.getScore(), robotCfg.getPlayerLevel()));
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
        boolean locked = false;
        try {
            locked = redisLock.tryLock(lockKey, MAX_WAIT_TIME);
            if (locked) {
                robotDao.recycleRobotPlayers(Map.of(robotId, getRobotRealMoney(robotId)));
            }
        } catch (InterruptedException ignored) {
        } finally {
            if (locked) {
                redisLock.tryUnlock(lockKey);
            }
        }
    }


    /**
     * 删除机器人
     */
    public void recycleRobotPlayers(List<Long> robotIds) {
        String lockKey = robotDao.getLockRobotTableName();
        Map<Long, Pair<Integer, Long>> collect = getRobotRealMoney(robotIds.stream());
        boolean locked = false;
        try {
            locked = redisLock.tryLock(lockKey, MAX_WAIT_TIME);
            if (locked) {
                robotDao.recycleRobotPlayers(collect);
            }
        } catch (InterruptedException ignored) {
        } finally {
            if (locked) {
                redisLock.tryUnlock(lockKey);
            }
        }
    }

    public Map<Long, Pair<Integer, Long>> getRobotRealMoney(Stream<Long> robotIdsStream) {
        return robotIdsStream
                .map(id -> GameDataManager.getRobotCfg(id.intValue()))
                .collect(Collectors.toMap(cfg -> (long) cfg.getId(), this::getRobotRealMoney));
    }

    /**
     * 当服务器关闭时，删除当前服的所有机器人
     */
    public void deleteServerAllRobot() {
        String nodePath = nodeManager.getNodePath();
        String lockKey = robotDao.getLockRobotTableName();
        boolean locked = false;
        try {
            locked = redisLock.tryLock(lockKey, MAX_WAIT_TIME);
            if (locked) {
                //
                Set<Long> allRobots = robotDao.getCurServerRobotPlayers();
                if (allRobots == null || allRobots.isEmpty()) {
                    return;
                }
                String serverRobotTableName = robotDao.getCurServerRobotTableName();
                // 删除数据

                robotDao.recycleRobotPlayers(getRobotRealMoney(allRobots.stream()));
                log.info("删除机器人Redis数据库：{} 中节点：{} 对应的机器人数据成功 删除数量：{}",
                        serverRobotTableName, nodePath, allRobots.size());
            }
        } catch (InterruptedException ignored) {
        } finally {
            if (locked) {
                redisLock.tryUnlock(lockKey);
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
    }

    private Pair<Integer, Long> getRobotRealMoney(long robotId) {
        RobotCfg robotCfg = GameDataManager.getRobotCfg((int) robotId);
        return getRobotRealMoney(robotCfg);
    }

    private Pair<Integer, Long> getRobotRealMoney(RobotCfg cfg) {
        return Pair.newPair(cfg.getPlayerLevel(), RandomUtils.randomWeightList(cfg.getAddMoney()));
    }

    /**
     * 初始化机器人ID库，预先将机器人数据写入机器人ID表中
     */
    public void checkOrInitRobotIdPool() {
        //机器人id 机器人最少金币
        Map<Long, Pair<Integer, Long>> robotCfgMap =
                GameDataManager.getRobotCfgList().stream()
                        // 所有的ID必须符合规则
                        .filter(cfg -> cfg.getId() % 17 == 0)
                        .collect(Collectors.toMap(cfg -> (long) cfg.getId(),
                                this::getRobotRealMoney));
        String lockKey = robotDao.getLockRobotTableName();
        boolean locked = false;
        try {
            locked = redisLock.tryLock(lockKey, MAX_WAIT_TIME);
            if (locked) {
                // 获取所有在使用的机器人表
                List<Long> databaseAllRobot = robotDao.getAllUsedRobot();
                // 移除所有数据库中的机器人ID
                databaseAllRobot.forEach(robotCfgMap.keySet()::remove);
                if (robotCfgMap.isEmpty()) {
                    // 如果没有新的机器人数据
                    return;
                }
                // 添加所有新增的机器人ID
                robotDao.addNewRobotIds(robotCfgMap);
                log.info("新增机器人ID数量： {}", robotCfgMap.size());
            }
        } catch (InterruptedException ignored) {
        } finally {
            if (locked) {
                redisLock.tryUnlock(lockKey);
            }
        }
    }

    @Override
    public void shutdown() {
    }
}
