package com.jjg.game.room.services;

import com.jjg.game.common.baselogic.ConsoleDebugger;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.room.AbstractRoomDao;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.exception.GameSampleException;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 房间服务器
 *
 * @author 2CL
 */
@Service
public class RoomService implements IRoomStartListener, TimerListener<IProcessorHandler> {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);
    @Autowired
    private RoomManager roomManager;
    @Autowired
    private RobotService robotService;
    @Autowired
    private TimerCenter timerCenter;
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
        timerCenter = new TimerCenter("room-start-timer");
        timerCenter.start();
        // 先删除服务器所有的机器人，如果服务器异常关闭，可以保证机器人不会被异常占用，能正常进入机器人池
        try {
            robotService.deleteServerAllRobot();
        } catch (Exception exception) {
            log.error("删除服务器所有的机器人时发生异常：{}", exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
        // 检查或初始化机器人池,创建房间之前需要所有的机器人ID就绪
        robotService.checkOrInitRobotIdPool();
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
     * 服务器启动时检查房间创建房间并初始化
     */
    private void checkCreateRoomAndInit() {
        Map<Integer, EGameType> availableGames = getAvailableGames();
        // 没有可用的直接退出
        if (availableGames == null || availableGames.isEmpty()) {
            return;
        }
        // 本地调试时使用
        if (ConsoleDebugger.isIdeModel()) {
            if (nodeConfig.getNeedBootGameId() != null) {
                List<Integer> needBootGameList = new ArrayList<>();
                for (int gameId : nodeConfig.getNeedBootGameId()) {
                    needBootGameList.add(gameId);
                }
                availableGames.keySet().removeIf(a -> !needBootGameList.contains(a));
            }
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
            List<Integer> deletionSolution = warehouseCfg.getRoomDeletion_Solution();
            // 每个游戏最小存在的房间数量
            int minRoomNum = deletionSolution.getFirst();
            if (minRoomNum > 0) {
                // 有些游戏还暂未实现逻辑先跳过
                if (EGameType.getGameByTypeId(warehouseCfg.getGameID()) == null) {
                    log.warn("warehouseCfg表中游戏ID：{} 在EGameType中找不到定义", warehouseCfg.getGameID());
                    continue;
                }
                // 初始化房间的逻辑需要分散运行，让房间的所有逻辑分散执行
                addRandomTimeEvent(() -> checkRoomInit(warehouseCfg, minRoomNum));
            }
        }
        isInitialed = true;
    }

    /**
     * 添加随机定时任务
     */
    public void addRandomTimeEvent(IProcessorHandler processorHandler) {
        int initTime = RandomUtils.getRandomNumInt10000();
        timerCenter.add(new TimerEvent<>(this, initTime, processorHandler));
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
            // 先将已存在的房间启动起来
            boolean initRes = initRoom(warehouseCfg);
            // 再判断是否还缺房间，如果缺房间则创建新的房间
            int count = (int) roomDao.existRoomCount(gameType, warehouseCfg.getId());
            // 如果初始化失败需要重新创建一个房间
            if (!initRes || count < minRoomNum) {
                count = !initRes ? 0 : count;
                // 循环执行创建房间
                for (int i = count; i < minRoomNum; i++) {
                    // 如果房间不足需要创建房间, 分散创建避免同一时刻出现大量IO请求
                    addRandomTimeEvent(() -> createRoom(warehouseCfg));
                }
            }
        } catch (Exception exception) {
            log.error("房间类型：{} 启动时 创建或者初始化房间失败", warehouseCfg.getGameID(), exception);
            throw exception;
        }
    }

    /**
     * 初始化房间
     */
    private boolean initRoom(WarehouseCfg warehouseCfg) throws Exception {
        int maxLimit = getRoomMaxLimit(warehouseCfg);
        int gameType = warehouseCfg.getGameID();
        //如果之前没有，就要创建一个房间
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController =
                roomManager.initNodeExistRoom(gameType, warehouseCfg.getId(), maxLimit);
        if (roomController == null) {
            return false;
        }
        EGameType eGameType = EGameType.getGameByTypeId(gameType);
        log.info("初始化游戏类型：{} 已存在的房间成功！ID：{} RoomCfgId: {}",
                eGameType.getGameDesc(), roomController.getRoom().getId(), roomController.getRoom().getRoomCfgId());
        return true;
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
        EGameType eGameType = EGameType.getGameByTypeId(gameType);
        log.info("创建游戏类型：{} 的房间成功！ID：{} RoomCfgId: {}",
                eGameType.getGameDesc(), roomController.getRoom().getId(), roomController.getRoom().getRoomCfgId());
        if (roomController.checkRoomCanContinue() && roomController.getGameController().checkRoomCanStart()) {
            roomController.startGame();
            log.info("游戏启动成功  roomInfo: {}", roomController.getRoom().logStr());
        } else {
            log.error("游戏启动失败  roomInfo: {}", roomController.getRoom().logStr());
        }
    }


    /**
     * 获取房间的最大限制值
     */
    private int getRoomMaxLimit(WarehouseCfg warehouseCfg) {
        Tuple2<Integer, Integer> tuple2 = SampleDataUtils.getRoomMaxLimit(warehouseCfg);
        return tuple2.getT2();
    }

    @Override
    public void shutdown() {
        // 需要执行房间中的关闭逻辑
        if (!roomManager.isRoomStopping()) {
            roomManager.setRoomStopping(true);
            // 调用房间关闭逻辑
            roomManager.onServerShutdown();
            // 删除当前服务器的机器人
            robotService.deleteServerAllRobot();
        }
    }

    @Override
    public void onTimer(TimerEvent<IProcessorHandler> event) {
        if (event == null || event.getParameter() == null) {
            return;
        }
        try {
            // 执行定时任务
            event.getParameter().action();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
