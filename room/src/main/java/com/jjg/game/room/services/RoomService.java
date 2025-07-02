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

import java.util.List;

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


    @Override
    public int[] getGameTypes() {
        return new int[0];
    }


    /**
     * 服务器启动时检查房间通用逻辑
     */
    @Override
    public void start() {
        // 主节点去处理房间的初始化
        /*if (!marsCurator.master(NodeType.GAME.getValue())) {
            return;
        }*/
        // 判断房间删除逻辑
        List<WarehouseCfg> warehouseCfgs = GameDataManager.getWarehouseCfgList();
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
                // TODO 测试代码
                if (EGameType.BACCARAT.getGameTypeId() != warehouseCfg.getGameID()) {
                    continue;
                }
                // 暂时先按配置创建所有类型的房间
                checkRoomInit(warehouseCfg);
            }
        }
    }

    /**
     * 检查房间初始化
     */
    private void checkRoomInit(WarehouseCfg warehouseCfg) {
        String nodePath = marsCurator.nodePath;
        // TODO 主节点需要知道所有节点开启的游戏列表，并在此检查开启的游戏的房间初始化逻辑
        // TODO 如果原本游戏开了，但是后面配置将游戏关了，需要删除原有的初始房间,或在大厅走统一的房间删除逻辑
        int gameType = warehouseCfg.getGameID();
        AbstractRoomDao<Room, ? extends RoomPlayer> roomDao = roomManager.getRoomDao(gameType);
        if (roomDao == null) {
            log.warn("游戏类型：{} 找不到对应的RoomDao", gameType);
            return;
        }
        long count = roomDao.existRoomCount(gameType, warehouseCfg.getId());
        if (count < 1) {
            String participantsMax = warehouseCfg.getParticipants_max();
            String[] participantsMaxStrArr = participantsMax.split(":");
            int maxLimit = Integer.parseInt(participantsMaxStrArr[1]);
            EGameType eGameType = EGameType.getGameByTypeId(gameType);
            //如果之前没有，就要创建一个房间
            AbstractRoomController<? extends RoomCfg, ? extends Room> roomController =
                roomManager.createGameDefaultRoom(gameType,
                    warehouseCfg.getId(),
                    maxLimit,
                    eGameType.getRoomType());
            if (roomController == null) {
                return;
            }
            Room room = roomController.getRoom();
            long roomId = room.getId();
            // 创建一个机器人,将机器人放入到游戏中
            RobotPlayer robotPlayer = robotService.getOrCreateRobotPlayer(warehouseCfg.getId(), roomId);
            if (robotPlayer == null) {
                log.error("机器人创建失败!");
            }
            PFSession robotSession = new PFSession(null, null, null);
            robotSession.setGatePath(nodePath);
            PlayerController robotPlayerController = new PlayerController(robotSession, robotPlayer);
            // 机器人加入房间
            roomController.joinRoom(robotPlayerController);
        } else {
            log.debug("该游戏已有初始房间存在，无需创建房间 gameType = {}", gameType);
        }
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void isLeader(int gameType) {
        log.debug("当前游戏类型：{} 节点：{} 选举为master节点", gameType, clusterSystem.getNodePath());
    }

    @Override
    public void notLeader(int gameType) {

    }
}
