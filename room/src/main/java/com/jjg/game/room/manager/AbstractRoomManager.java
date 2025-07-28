package com.jjg.game.room.manager;

import com.jjg.game.common.cluster.ClusterProcessorExecutors;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.dao.PlayerRoomDataDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.match.MatchDataDao;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.utils.ReflectionTool;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.services.RobotService;
import com.jjg.game.room.timer.RoomTimerCenter;
import jakarta.annotation.PostConstruct;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 房间管理器管理所有的RoomController，RoomController管理自己的GameController数据
 *
 * @author 11
 * @date 2025/6/25 10:19
 */
public abstract class AbstractRoomManager implements ApplicationContextAware {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private boolean isStopping = false;
    @Autowired
    protected NodeManager nodeManager;
    @Autowired
    protected ClusterSystem clusterSystem;
    @Autowired
    protected CorePlayerService playerService;
    // 通过单例拿取
    protected ClusterProcessorExecutors processorExecutors = ClusterProcessorExecutors.getInstance();
    @Autowired
    private MatchDataDao matchDataDao;
    @Autowired
    private RobotService robotService;
    @Autowired
    private PlayerRoomDataDao playerRoomDataDao;
    // context
    protected ApplicationContext applicationContext;
    // 房间计时器(线程池)
    protected RoomTimerCenter timerCenter;
    // 不同类型的房间roomDao
    protected Map<Class<? extends Room>, AbstractRoomDao<? extends Room, ? extends RoomPlayer>> roomDaoMap
        = new HashMap<>();
    //所有的房间控制器  gameType -> roomId - > RoomController
    protected Map<Integer, Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>>> roomControllerMap =
        new ConcurrentHashMap<>();
    // 房间控制器class类集合
    protected Set<Class<? extends AbstractRoomController>> roomControllerClazz;
    // 游戏控制器class类集合
    protected Set<Class<? extends AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>>> gameControllerClazz;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public AbstractRoomManager() {
        this.timerCenter = new RoomTimerCenter("room-timer", processorExecutors);
        this.timerCenter.start();
    }

    @PostConstruct
    public <RC extends RoomCfg> void init() {
        try {
            Reflections reflections = new Reflections(CoreConst.Common.BASE_PROJECT_PACKAGE_PATH);
            // 房间控制器初始化
            Set<Class<? extends AbstractRoomController>> allRoomController =
                reflections.getSubTypesOf(AbstractRoomController.class);
            log.info("获取到房间控制器: {}", allRoomController.stream().map(Class::getSimpleName).collect(Collectors.joining(
                ",")));
            roomControllerClazz = allRoomController;
            // 房间数据Dao初始化
            Set<Class<? extends AbstractRoomDao>> allRoomDao = reflections.getSubTypesOf(AbstractRoomDao.class);
            for (Class<? extends AbstractRoomDao> aClass : allRoomDao) {
                if (aClass.isInterface() || Modifier.isAbstract(aClass.getModifiers())) {
                    continue;
                }
                Set<Class<Room>> roomDaoTypeParams = ReflectionTool.getClassSuperActualType(aClass);
                Class<Room> roomClass = null;
                for (Class<Room> roomDaoTypeParam : roomDaoTypeParams) {
                    if (Room.class.isAssignableFrom(roomDaoTypeParam)) {
                        roomClass = roomDaoTypeParam;
                        break;
                    }
                }
                // 从Spring中获取所有的Dao实例
                AbstractRoomDao<? extends Room, ? extends RoomPlayer> roomDao = applicationContext.getBean(aClass);
                if (roomClass != null) {
                    roomDaoMap.put(roomClass, roomDao);
                } else {
                    log.error("RoomDao：{} 找不到对应的房间数据类", aClass.getSimpleName());
                }
            }
            // 所有的游戏控制器
            Set<Class<?>> annotatedClasses =
                reflections.get(Scanners.TypesAnnotated.with(GameController.class).asClass());
            gameControllerClazz =
                annotatedClasses
                    .stream()
                    .map(aClass -> (Class<AbstractGameController<RC, ? extends GameDataVo<RC>>>) aClass)
                    .filter(aClass ->
                        !aClass.isInterface() && !Modifier.isAbstract(aClass.getModifiers()))
                    .collect(Collectors.toSet());
        } catch (Exception exception) {
            log.error("获取房间控制器失败", exception);
            throw exception;
        }
    }

    /**
     * 通过游戏配置ID获取房间的实际配置
     */
    private <RC extends RoomCfg> RC getRoomActualCfg(int gameCfgId, int gameType) {
        EGameType eGameType = EGameType.getGameByTypeId(gameType);
        RoomType roomType = eGameType.getRoomType();
        switch (roomType) {
            case BET_ROOM -> {
                return (RC) GameDataManager.getRoom_BetCfg(gameCfgId);
            }
            case POKER_ROOM -> {
                return (RC) GameDataManager.getRoom_ChessCfg(gameCfgId);
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * 初始化已经存在的房间
     */
    public <RC extends RoomCfg, R extends Room> AbstractRoomController<RC, R> initExistRoom(
        int gameType, int roomCfgId, int maxLimit) throws Exception {
        EGameType eGameType = EGameType.getGameByTypeId(gameType);
        RoomType roomType = eGameType.getRoomType();
        // 获取roomDao
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDaoByType(gameType);
        // TODO 每个游戏类型都会有对应的Room去处理相应的数据，现在没有先退出
        if (roomDao == null) {
            return null;
        }
        List<Object> roomIds = roomDao.getAllRoomIds(gameType, roomCfgId);
        if (roomIds == null || roomIds.isEmpty()) {
            log.error("初始化房间时房间ID为空");
            return null;
        }
        Integer roomId = (Integer) RandomUtils.randCollection(roomIds);
        if (roomId == null || roomId == 0) {
            return null;
        }
        R room = roomDao.getRoom(gameType, roomId);
        return initWithRoom(gameType, roomCfgId, maxLimit, roomType, room);
    }

    /**
     * 创建初始的系统房间,有些游戏开服就会有默认的房间
     */
    public <RC extends RoomCfg, R extends Room> AbstractRoomController<RC, R> createGameDefaultRoom(
        int gameType, int roomCfgId, int maxLimit) throws Exception {
        EGameType eGameType = EGameType.getGameByTypeId(gameType);
        RoomType roomType = eGameType.getRoomType();
        // 获取roomDao
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDaoByType(gameType);
        // TODO 每个游戏类型都会有对应的Room去处理相应的数据，现在没有先退出
        if (roomDao == null) {
            return null;
        }
        R room = roomDao.nodeCreate(gameType, roomCfgId, maxLimit, this.nodeManager.getNodePath());
        if (room == null) {
            log.error("创建房间失败 gameType = {},roomCfgId = {},roomType = {}", gameType, roomCfgId, roomType);
            return null;
        }
        return initWithRoom(gameType, roomCfgId, maxLimit, roomType, room);
    }

    /**
     * 通过游戏类型ID查找对应的RoomDao
     */
    private <R extends Room> AbstractRoomDao<R, ? extends RoomPlayer> getRoomDaoByType(int gameType) {
        EGameType eGameType = EGameType.getGameByTypeId(gameType);
        RoomType roomType = eGameType.getRoomType();
        // 获取roomDao
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDao(roomType);
        if (roomDao == null) {
            log.error("游戏类型：{} 对应的房间类型：{} 找不到对应的RoomDao", gameType, roomType);
            return null;
        }
        return roomDao;
    }

    /**
     * 通过房间数据初始房间
     */
    private <RC extends RoomCfg, R extends Room> AbstractRoomController<RC, R> initWithRoom(
        int gameType, int roomCfgId, int maxLimit, RoomType roomType, R room) throws Exception {
        // 房间配置
        RC roomCfg = getRoomActualCfg(roomCfgId, gameType);
        if (roomCfg == null) {
            log.error("节点创建房间，房间类型: {} 房间ID：{} 找不到配置", gameType, roomCfgId);
            return null;
        }
        // 创建房间控制器
        AbstractRoomController<RC, R> roomController = createRoomController(gameType, room, roomCfg);
        // 注册房间控制器
        registerRoomController(gameType, room.getId(), roomController);

        log.debug("系统创建房间成功 gameType = {},roomCfgId = {},roomType = {},maxLimit = {}",
            gameType, roomCfgId, roomType, maxLimit);
        return roomController;
    }

    /**
     * player加入房间
     */
    public <RC extends RoomCfg, R extends Room> int joinRoom(PlayerController playerController, int gameType,
                                                             long roomId) {
        try {
            if (roomId < 1) {
                log.debug("roomId不能小于,加入房间失败 gameType = {},roomId = {} ,playerId = {}", gameType, roomId,
                    playerController.playerId());
                return Code.FAIL;
            }

            AbstractRoomController<RC, R> roomController = getRoomController(gameType, roomId);
            if (roomController == null) {
                AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDao(gameType);
                //从redis获取房间数据
                R room = roomDao.getRoom(gameType, roomId);
                if (room == null) {
                    log.warn("加入房间失败，该房间不存在 gameType = {},roomId = {},playerId = {}", gameType, roomId,
                        playerController.playerId());
                    return Code.FAIL;
                }

                //如果该房间所在节点不是本节点，就要切换节点
                if (!room.getPath().equals(this.nodeManager.getNodePath())) {
                    MarsNode node = clusterSystem.getNode(room.getPath());
                    if (node == null) {
                        log.warn("加入房间成功，开始切换节点 gameType = {},roomId = {},playerId = {},toRoomPath = {}", gameType,
                            roomId, playerController.playerId(), room.getPath());
                        return Code.FAIL;
                    }
                    clusterSystem.switchNode(playerController.getSession(), node);
                    return Code.SUCCESS;
                }
                int roomCfgId = room.getRoomCfgId();
                RC roomCfg = getRoomActualCfg(roomCfgId, gameType);
                if (roomCfg == null) {
                    log.error("房间类型: {} 找不到配置", roomCfgId);
                    return Code.SAMPLE_ERROR;
                }
                //如果是本节点，先创建roomController
                roomController = createRoomController(gameType, room, roomCfg);
                registerRoomController(gameType, roomId, roomController);
            }

            //roomController不为空，那么room就是在本节点
            CommonResult<R> addResult = roomController.joinRoom(playerController);
            if (!addResult.success()) {
                return Code.FAIL;
            }
            roomController.setRoom(addResult.data);
            playerController.setPlayer(playerService.doSave(playerController.playerId(), p -> p.setRoomId(roomId)));
            log.debug("玩家加入房间成功 gameType = {},roomId = {}, playerId = {}", gameType, roomId,
                playerController.playerId());
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("玩家：{} 房间类型：{} 房间ID: {} 加入发生异常", playerController.playerId(), gameType, roomId, e);
        }
        return Code.FAIL;
    }

    /**
     * 玩家退出房间
     */
    public <RC extends RoomCfg, R extends Room> int exitRoom(PlayerController playerController) {
        try {
            if (playerController.roomId() < 1) {
                log.debug("roomId不能小于,退出房间失败 gameType = {},playerId = {}", playerController.getPlayer().getGameType(),
                    playerController.playerId());
                return Code.FAIL;
            }

            AbstractRoomController<RC, R> roomController = getRoomController(playerController.getPlayer().getGameType(),
                playerController.roomId());
            if (roomController == null) {
                log.warn("退出房间失败，该房间不存在 gameType = {},roomId = {},playerId = {}",
                    playerController.getPlayer().getGameType(), playerController.roomId(), playerController.playerId());
                return Code.FAIL;
            }
            // 房间控制器退出房间逻辑
            CommonResult<Room> roomResult = roomController.onPlayerLeaveRoom(playerController);
            if (!roomResult.success()) {
                return roomResult.code;
            }
            // TODO 需要添加定时 检查是否销毁房间
            // 退出房间将当前场景置为空
            playerController.setScene(null);
            // 将玩家房间ID置为0
            playerService.doSave(playerController.playerId(), p -> p.setRoomId(0));
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("", e);
        }
        return Code.FAIL;
    }

    /**
     * 解散房间
     *
     * @param gameType 游戏类型
     * @param roomId   房间ID
     */
    public <R extends Room> void disbandRoom(R room, int gameType, long roomId) {
        if (!roomControllerMap.containsKey(gameType)) {
            log.error("房间解散时，房间管理器不存在游戏类型: {} 的房间控制器集合", gameType);
            return;
        }
        Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllers =
            roomControllerMap.get(gameType);
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController =
            roomControllers.get(roomId);
        if (roomController == null) {
            log.error("解散放假时，通过房间ID: {} 查找房间管理器不存在", roomId);
            return;
        }
        // 调用解散房间控制器中的逻辑
        roomController.disbandRoom();
        // 回存房间相关数据
        // 移除房间map中的数据
        roomControllers.remove(roomId);
        // 刪除房间
        deleteRoomFromRedis(room);
    }

    /**
     * 从redis中删除房间信息,当房间清除时需要清除所有redis中有引用到该房间信息的地方
     */
    private <R extends Room> void deleteRoomFromRedis(R room) {
        // 从redis中删除房间信息
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao =
            (AbstractRoomDao<R, ? extends RoomPlayer>) getRoomDao(room.getClass());
        roomDao.removeRoom(room.getGameType(), room.getId(), room.getRoomCfgId());
        // 需要从房间等待列表中删除
        matchDataDao.removeWaitJoinRoomId(room.getGameType(), room.getRoomCfgId(), room.getId());
    }

    /**
     * 清除房间
     */
    public void clearRoom() {

    }

    /**
     * 服务器关闭时
     */
    public void serverShutdown() {
        // 通知所有的房间
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> values : roomControllerMap.values()) {
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : values.values()) {
                roomController.serverShutdown();
            }
        }
    }

    /**
     * 获取 RoomController
     */
    protected <RC extends RoomCfg, R extends Room> AbstractRoomController<RC, R> getRoomController(
        int gameType, long roomId) {
        Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> tempMap =
            this.roomControllerMap.get(gameType);
        if (tempMap == null || tempMap.isEmpty()) {
            return null;
        }

        return (AbstractRoomController<RC, R>) tempMap.get(roomId);
    }

    /**
     * 注册房间控制器
     */
    protected <RC extends RoomCfg, R extends Room> void registerRoomController(
        int gameType, long roomId, AbstractRoomController<RC, R> roomController) {
        this.roomControllerMap.computeIfAbsent(gameType, k -> new ConcurrentHashMap<>()).computeIfAbsent(roomId,
            k -> roomController);
    }

    /**
     * 通过房间类型创建房间控制器
     */
    protected <RC extends RoomCfg, R extends Room> AbstractRoomController<RC, R> createRoomController(
        int gameType, Room room, RC roomCfg) throws Exception {
        Class<? extends RoomPlayer> roomPlayerClass = RoomPlayer.class;
        // 通过游戏类型找到对应的房间控制器
        EGameType game = EGameType.getGameByTypeId(gameType);
        RoomType roomType = game.getRoomType();
        Class<? extends Room> roomDataTypeClass = roomType.getRoomDataType();
        Class<AbstractRoomController<RC, R>> targetRoomControllerClass = null;
        for (Class<? extends AbstractRoomController> controllerClazz : this.roomControllerClazz) {
            // 获取房间控制器上的房间数据类型
            Set<Class<Object>> roomDataClasses = ReflectionTool.getClassSuperActualType(controllerClazz);
            Class<? extends Room> roomDataClass = null;
            for (Class<?> clazz : roomDataClasses) {
                if (Room.class.isAssignableFrom(clazz)) {
                    roomDataClass = (Class<R>) clazz;
                    break;
                }
            }
            if (roomDataTypeClass.equals(roomDataClass)) {
                targetRoomControllerClass = (Class<AbstractRoomController<RC, R>>) controllerClazz;
            }
        }
        // 按道理如果是从EGames中获取的，并且是从RoomType中链接过来的Controller不应该找不到
        assert targetRoomControllerClass != null;
        Constructor<AbstractRoomController<RC, R>> controllerConstructor =
            targetRoomControllerClass.getDeclaredConstructor(Class.class, roomDataTypeClass);
        AbstractRoomController<RC, R> roomController = controllerConstructor.newInstance(roomPlayerClass, room);
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDao(roomType);
        if (roomDao != null) {
            roomController.setRoomDao(roomDao);
        } else {
            log.error("房间数据类: {} 未找到对应的Dao", roomType.getRoomDataType().getSimpleName());
        }
        roomController.setRoomCfg(roomCfg);
        roomController.setTimerCenter(this.timerCenter);
        roomController.setRoomManager(this);
        // 调用房间管理器的初始化方法
        roomController.initGame();
        return roomController;
    }

    /**
     * 通过房间数据类获取对应的房间Dao
     */
    public <R extends Room> AbstractRoomDao<R, ? extends RoomPlayer> getRoomDao(Class<R> rClass) {
        return (AbstractRoomDao<R, ? extends RoomPlayer>) roomDaoMap.get(rClass);
    }

    /**
     * 通过房间类型获取对应的房间Dao
     */
    public <R extends Room> AbstractRoomDao<R, ? extends RoomPlayer> getRoomDao(RoomType roomType) {
        Class<R> roomClass = (Class<R>) roomType.getRoomDataType();
        return (AbstractRoomDao<R, ? extends RoomPlayer>) roomDaoMap.get(roomClass);
    }

    /**
     * 通过房间类型获取对应的房间Dao
     */
    public <R extends Room> AbstractRoomDao<R, ? extends RoomPlayer> getRoomDao(int gameType) {
        EGameType games = EGameType.getGameByTypeId(gameType);
        RoomType roomType = games.getRoomType();
        Class<R> roomClass = (Class<R>) roomType.getRoomDataType();
        return (AbstractRoomDao<R, ? extends RoomPlayer>) roomDaoMap.get(roomClass);
    }

    /**
     * 游戏控制器class
     */
    public Set<Class<? extends AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>>> getGameControllerClazz() {
        return gameControllerClazz;
    }

    public ClusterProcessorExecutors getProcessorExecutors() {
        return processorExecutors;
    }

    public MatchDataDao getMatchDataDao() {
        return matchDataDao;
    }

    public RobotService getRobotService() {
        return robotService;
    }

    public PlayerRoomDataDao getPlayerRoomDataDao() {
        return playerRoomDataDao;
    }

    /**
     * 通过玩家ID获取到玩家所在的GameController
     */
    public AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> getGameControllerByPlayerId(long playerId) {
        List<Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>>> roomMapControllers =
            new ArrayList<>(roomControllerMap.values());
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllerMap :
            roomMapControllers) {
            List<AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllers =
                new ArrayList<>(roomControllerMap.values());
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : roomControllers) {
                if (roomController.getPlayerControllers().containsKey(playerId)) {
                    return roomController.getGameController();
                }
            }
        }
        return null;
    }

    /**
     * 通过玩家ID获取到玩家所在的GameController
     */
    public AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> getGameControllerByRoomId(long roomId) {
        List<Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>>> roomMapControllers =
            new ArrayList<>(roomControllerMap.values());
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllerMap :
            roomMapControllers) {
            List<AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllers =
                new ArrayList<>(roomControllerMap.values());
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : roomControllers) {
                if (roomController.getRoom().getId() == roomId) {
                    return roomController.getGameController();
                }
            }
        }
        return null;
    }

    /**
     * 通过游戏类型获取所有的该类型的游戏控制器
     */
    public List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> getGameControllersByGameType(EGameType gameType) {
        List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> gameControllers =
            new ArrayList<>();
        List<Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>>> roomMapControllers =
            new ArrayList<>(roomControllerMap.values());
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllerMap :
            roomMapControllers) {
            List<AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllers =
                new ArrayList<>(roomControllerMap.values());
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : roomControllers) {
                if (roomController.getGameController().gameControlType().equals(gameType)) {
                    gameControllers.add(roomController.getGameController());
                }
            }
        }
        return gameControllers;
    }

    public boolean isStopping() {
        return isStopping;
    }

    public void setStopping(boolean stopping) {
        isStopping = stopping;
    }
}
