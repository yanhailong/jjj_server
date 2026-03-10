package com.jjg.game.room.services;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.room.AbstractRoomDao;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.exception.GameSampleException;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 房间服务器
 *
 * @author 2CL
 */
@Service
public class RoomService implements IRoomStartListener {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);
    @Autowired
    private RoomManager roomManager;
    private boolean isInitialed = false;
    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private TaskManager taskManager;

    /**
     * 服务器启动时检查房间通用逻辑
     */
    @Override
    public void start() {
        if (isInitialed) {
            return;
        }
        // 检查房间配置是否有错误
        checkRoomSampleData();
        // 检查房间的创建和初始化
        try {
            checkCreateRoomAndInit();
        } catch (Exception e) {
            log.error("房间初始化时异常：{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        //初始化任务
        taskManager.init();
    }

    /**
     * 服务器启动时同步检查房间并初始化系统房
     */
    private void checkCreateRoomAndInit() {
        Map<Integer, EGameType> availableGames = getAvailableGames();
        // 没有可用的直接退出
        if (availableGames == null || availableGames.isEmpty()) {
            return;
        }
        // 本地调试时使用
        if (nodeConfig.getNeedBootGameId() != null) {
            List<Integer> needBootGameList = new ArrayList<>();
            for (int gameId : nodeConfig.getNeedBootGameId()) {
                needBootGameList.add(gameId);
            }
            availableGames.keySet().removeIf(a -> !needBootGameList.contains(a));
        }
        String openedGames =
                availableGames.values().stream().map(EGameType::getGameDesc).collect(Collectors.joining(","));
        log.info("准备开始初始化游戏房间【{}】", openedGames);
        List<WarehouseCfg> warehouseCfgs =
                GameDataManager.getWarehouseCfgList()
                        .stream()
                        .filter(warehouseCfg -> availableGames.containsKey(warehouseCfg.getGameID()))
                        .toList();
        if (warehouseCfgs.isEmpty()) {
            return;
        }
        for (WarehouseCfg warehouseCfg : warehouseCfgs) {
            // 组队类型的房间直接跳过
            if (warehouseCfg.getRoomType() >= GameConstant.RoomTypeCons.FRIEND_ROOM_TYPE_START) {
                continue;
            }
            //清除老的数据的房间信息
            int gameType = warehouseCfg.getGameID();
            roomManager.clearNodeExistRoom(gameType, warehouseCfg.getId());
            List<Integer> deletionSolution = warehouseCfg.getRoomDeletion_Solution();
            // 每个游戏最小存在的房间数量
            int minRoomNum = deletionSolution.getFirst();
            if (minRoomNum > 0) {
                // 有些游戏还暂未实现逻辑先跳过
                if (EGameType.getGameByTypeId(warehouseCfg.getGameID()) == null) {
                    log.warn("warehouseCfg表中游戏ID：{} 在EGameType中找不到定义", warehouseCfg.getGameID());
                    continue;
                }
                try {
                    checkRoomInit(warehouseCfg, minRoomNum);
                } catch (Exception e) {
                    log.error("创建房间异常 gameType:{} wareId:{} ", warehouseCfg.getGameID(), warehouseCfg.getId());
                }
            }
        }
        isInitialed = true;
    }

    /**
     * 获取当前服务器可用的游戏类型
     */
    public Map<Integer, EGameType> getAvailableGames() {
        Set<EGameType> availableTypes = roomManager.getGameAvailableTypes();
        if (availableTypes.isEmpty()) {
            return Collections.emptyMap();
        }
        return availableTypes.stream().
                collect(HashMap::new, (map, e) -> map.put(e.getGameTypeId(), e), HashMap::putAll);
    }

    /**
     * 检查房间初始化
     */
    private void checkRoomInit(WarehouseCfg warehouseCfg, int minRoomNum) throws Exception {
        int gameType = warehouseCfg.getGameID();
        AbstractRoomDao<Room, ? extends RoomPlayer> roomDao = roomManager.getRoomDao(warehouseCfg.getId());
        if (roomDao == null) {
            log.warn("游戏类型：{} 找不到对应的RoomDao", gameType);
            return;
        }
        try {
            // 按保底数量同步创建系统房
            for (int i = 0; i < minRoomNum; i++) {
                // 逐个创建，避免初始化逻辑分散到多条路径
                createRoom(warehouseCfg);
            }
        } catch (Exception exception) {
            log.error("房间类型：{} 启动时 创建或者初始化房间失败", warehouseCfg.getGameID(), exception);
            throw exception;
        }
    }


    /**
     * 创建房间
     */
    private void createRoom(WarehouseCfg warehouseCfg) throws Exception {
        roomManager.createAndStartDefaultRoom(warehouseCfg, "启动初始化");
    }

    @Override
    public void shutdown() {
        // 需要执行房间中的关闭逻辑
        if (!roomManager.isRoomStopping()) {
            roomManager.setRoomStopping(true);
            // 调用房间关闭逻辑
            roomManager.onServerShutdown();
        }
    }


    public void checkRoomSampleData() {
        List<Room_BetCfg> roomBetCfgs = GameDataManager.getRoom_BetCfgList();
        for (Room_BetCfg roomBetCfg : roomBetCfgs) {
            if (roomBetCfg.getTransactionItemId() == 0) {
                throw new GameSampleException(Room_BetCfg.EXCEL_NAME + " ID：" + roomBetCfg.getId() + " 货币ID为0");
            }
        }
    }
}
