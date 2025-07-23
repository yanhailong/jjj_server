package com.jjg.game.room.services;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.sample.bean.WarehouseCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 房间服务器
 *
 * @author 2CL
 */
@Service
public class RoomService implements IRoomStartListener, IGameClusterLeaderListener {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);
    @Autowired
    private RoomManager roomManager;
    @Autowired
    private MarsCurator marsCurator;
    @Autowired
    private RobotService robotService;
    @Autowired
    private ClusterSystem clusterSystem;
    private boolean isInitialed = false;
    /**
     * 当前节点是某些主节点游戏类型
     */
    private final Set<Integer> masterGameTypes = new CopyOnWriteArraySet<>();

    @Override
    public int[] getGameTypes() {
        return new int[0];
    }


    /**
     * 服务器启动时检查房间通用逻辑
     */
    @Override
    public void start() {
        if (isInitialed) {
            return;
        }
        // 检查房间的创建和初始化
        try {
            checkCreateRoomAndInit();
        } catch (Exception e) {
            log.error("房间初始化时异常：{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 服务器启动时检查房间创建房间并初始化
     */
    private void checkCreateRoomAndInit() throws Exception {
        List<WarehouseCfg> warehouseCfgs =
            GameDataManager.getWarehouseCfgList()
                .stream()
                .filter(warehouseCfg -> masterGameTypes.contains(warehouseCfg.getGameID()))
                .toList();
        if (warehouseCfgs.isEmpty()) {
            return;
        }
        for (WarehouseCfg warehouseCfg : warehouseCfgs) {
            List<Integer> deletionSolution = warehouseCfg.getRoomDeletion_Solution();
            // 每个游戏最小存在的房间数量
            int minRoomNum = deletionSolution.get(0);
            if (minRoomNum > 0) {
                // 有些游戏还暂未实现逻辑先跳过
                if (EGameType.getGameByTypeId(warehouseCfg.getGameID()) == null) {
                    log.warn("warehouseCfg表中游戏ID：{} 在EGameType中找不到定义", warehouseCfg.getGameID());
                    continue;
                }
                // 暂时先按配置创建所有类型的房间
                checkRoomInit(warehouseCfg, minRoomNum);
            }
            isInitialed = true;
        }
    }

    /**
     * 检查房间初始化
     */
    private void checkRoomInit(WarehouseCfg warehouseCfg, int minRoomNum) throws Exception {
        // TODO 主节点需要知道所有节点开启的游戏列表，并在此检查开启的游戏的房间初始化逻辑
        // TODO 如果节点启动时变成了主节点，节点需要在加载已存在的房间时判断是否已经被其他节点加载了
        int gameType = warehouseCfg.getGameID();
        AbstractRoomDao<Room, ? extends RoomPlayer> roomDao = roomManager.getRoomDao(gameType);
        if (roomDao == null) {
            log.warn("游戏类型：{} 找不到对应的RoomDao", gameType);
            return;
        }
        try {
            // 先将已存在的房间启动起来
            initRoom(warehouseCfg);
            // 再判断是否还缺房间，如果缺房间则创建新的房间
            long count = roomDao.existRoomCount(gameType, warehouseCfg.getId());
            if (count < minRoomNum) {
                // 如果房间不足需要创建房间
                createRoom(warehouseCfg);
            }
        } catch (Exception exception) {
            log.error("房间类型：{} 启动时 创建或者初始化房间失败", warehouseCfg.getGameID(), exception);
            throw exception;
        }
    }

    /**
     * 初始化房间
     */
    private void initRoom(WarehouseCfg warehouseCfg) throws Exception {
        int maxLimit = getRoomMaxLimit(warehouseCfg);
        int gameType = warehouseCfg.getGameID();
        //如果之前没有，就要创建一个房间
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController =
            roomManager.initExistRoom(gameType, warehouseCfg.getId(), maxLimit);
        if (roomController == null) {
            return;
        }
        startGameWithRoomController(roomController, warehouseCfg);
    }

    /**
     * 创建房间
     */
    private void createRoom(WarehouseCfg warehouseCfg) throws Exception {
        int gameType = warehouseCfg.getGameID();
        int maxLimit = getRoomMaxLimit(warehouseCfg);
        //如果之前没有，就要创建一个房间
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController =
            roomManager.createGameDefaultRoom(gameType, warehouseCfg.getId(), maxLimit);
        if (roomController == null) {
            return;
        }
        startGameWithRoomController(roomController, warehouseCfg);
    }

    /**
     * 添加机器人，通过房间控制器开始游戏
     */
    private void startGameWithRoomController(
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController, WarehouseCfg warehouseCfg) throws Exception {
        int gameType = warehouseCfg.getGameID();
        Room room = roomController.getRoom();
        long roomId = room.getId();
        // 创建一个机器人,将机器人放入到游戏中
        RobotPlayer robotPlayer = robotService.getOrCreateRobotPlayer(warehouseCfg.getId(), roomId);
        if (robotPlayer != null) {
            String nodePath = marsCurator.nodePath;
            PFSession robotSession = new PFSession(null, null, null);
            robotSession.setGatePath(nodePath);
            PlayerController robotPlayerController = new PlayerController(robotSession, robotPlayer);
            // 机器人加入房间
            roomController.joinRoom(robotPlayerController);
            log.info("创建游戏类型：{} 房间ID：{} 并加入初始机器人：{}", gameType, roomId, robotPlayer.getId());
        }
    }

    /**
     * 获取房间的最大限制值
     */
    private int getRoomMaxLimit(WarehouseCfg warehouseCfg) {
        String participantsMax = warehouseCfg.getParticipants_max();
        String[] participantsMaxStrArr = participantsMax.split(":");
        return Integer.parseInt(participantsMaxStrArr[1]);
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void isLeader(int gameType) {
        log.debug("当前游戏类型：{} 节点：{} 选举为master节点", gameType, clusterSystem.getNodePath());
        masterGameTypes.add(gameType);
        // 如果节点选举在Spring执行start方法之后会出问题，所以手动调用一次
        start();
    }

    @Override
    public void notLeader(int gameType) {

    }
}
