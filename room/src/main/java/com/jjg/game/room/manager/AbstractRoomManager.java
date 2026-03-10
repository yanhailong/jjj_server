package com.jjg.game.room.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.activity.wealthroulette.controller.WealthRouletteController;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.ReflectUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.room.AbstractRoomDao;
import com.jjg.game.core.dao.room.FriendRoomBillHistoryDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.match.MatchDataDao;
import com.jjg.game.core.pb.ResExitGame;
import com.jjg.game.core.recharge.service.RechargeService;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.datatrack.RoomDataTrackLogger;
import com.jjg.game.room.friendroom.AbstractFriendRoomController;
import com.jjg.game.room.services.RobotService;
import com.jjg.game.room.timer.RoomTimerCenter;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import jakarta.annotation.PostConstruct;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 房间管理器管理所有的RoomController，RoomController管理自己的GameController数据
 *
 * @author 11
 * @date 2025/6/25 10:19
 */
public abstract class AbstractRoomManager implements ApplicationContextAware, ConfigExcelChangeListener,
        TimerListener<IProcessorHandler> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    // 房间管理器是否处于房间关闭流程中
    private boolean isRoomStopping = false;
    @Autowired
    protected NodeManager nodeManager;
    @Autowired
    protected ClusterSystem clusterSystem;
    @Autowired
    protected CorePlayerService playerService;
    protected PlayerExecutorGroupDisruptor processorExecutors;
    @Autowired
    private MatchDataDao matchDataDao;
    @Autowired
    private RobotService robotService;
    @Autowired
    private RoomDataTrackLogger roomDataTrackLogger;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    protected FriendRoomBillHistoryDao friendRoomBillHistoryDao;
    @Autowired
    protected ActivityManager activityManager;
    @Autowired
    protected GameEventManager gameEventManager;
    @Autowired
    protected TaskManager taskManager;
    @Autowired
    protected WealthRouletteController wealthRouletteController;
    // context
    protected ApplicationContext applicationContext;
    // 房间计时器(线程池)
    protected RoomTimerCenter roomTimerCenter;
    // 房间管理器timer,多线程，非房间线程，如果需要调用房间相关的逻辑，需要抛到对应的房间线程
    protected TimerCenter roomManagerTimer;
    @Autowired
    protected MailService mailService;
    @Autowired
    protected CoreMarqueeManager coreMarqueeManager;
    @Autowired
    protected CoreLogger coreLogger;
    @Autowired
    protected RechargeService rechargeService;

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
        processorExecutors = PlayerExecutorGroupDisruptor.getDefaultExecutor();
        this.roomTimerCenter = new RoomTimerCenter("room-timer", processorExecutors);
        this.roomTimerCenter.start();
        this.roomManagerTimer = new TimerCenter("room-manager-timer");
        this.roomManagerTimer.start();
        // 添加空房间检查
        this.roomManagerTimer.add(new TimerEvent<>(this, this::emptyRoomCheck, 5000));
    }

    public WealthRouletteController getWealthRouletteController() {
        return wealthRouletteController;
    }

    public ActivityManager getActivityManager() {
        return activityManager;
    }

    public NodeManager getNodeManager() {
        return nodeManager;
    }

    public CoreMarqueeManager getCoreMarqueeManager() {
        return coreMarqueeManager;
    }

    @PostConstruct
    public <RC extends RoomCfg> void init() {
        try {
            Reflections reflections = new Reflections(CoreConst.Common.BASE_PROJECT_PACKAGE_PATH);
            // 房间控制器初始化
            Set<Class<? extends AbstractRoomController>> allRoomController =
                    reflections.getSubTypesOf(AbstractRoomController.class).stream()
                            .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                            .collect(Collectors.toSet());
            log.info("获取到房间控制器: {}", allRoomController.stream().map(Class::getSimpleName).collect(Collectors.joining(
                    ",")));
            roomControllerClazz = allRoomController;
            // 房间数据Dao初始化
            Set<Class<? extends AbstractRoomDao>> allRoomDao = reflections.getSubTypesOf(AbstractRoomDao.class);
            for (Class<? extends AbstractRoomDao> aClass : allRoomDao) {
                if (aClass.isInterface() || Modifier.isAbstract(aClass.getModifiers())) {
                    continue;
                }
                Set<Class<Room>> roomDaoTypeParams = ReflectUtils.getClassSuperActualType(aClass);
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
    private <RC extends RoomCfg> RC getRoomActualCfg(int gameCfgId) {
        RoomType roomType = RoomType.getRoomType(gameCfgId);
        switch (roomType) {
            case BET_TEAM_UP_ROOM, BET_ROOM -> {
                return (RC) GameDataManager.getRoom_BetCfg(gameCfgId);
            }
            case POKER_TEAM_UP_ROOM, POKER_ROOM -> {
                return (RC) GameDataManager.getRoom_ChessCfg(gameCfgId);
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * 删除当前节点已经存在的房间，此为兼容逻辑，按道理在服务器关闭时会清除当前节点所有的房间数据
     */
    public <R extends Room> void clearNodeExistRoom(
            int gameType, int roomCfgId) {
        // 获取roomDao
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDao(roomCfgId);
        if (roomDao == null) {
            return;
        }
        // 获取当前节点的所有房间
        List<R> nodeRoom = roomDao.getCurrentNodeRoom(gameType, roomCfgId);
        if (nodeRoom == null || nodeRoom.isEmpty()) {
            log.info("当前节点：{} 游戏类型：{} 的房间为空", nodeManager.getNodePath(), roomCfgId);
        } else {
            //不要系统创建房间的老数据直接删除相关数据
            for (R r : nodeRoom) {
                deleteRoomFromRedis(r);
            }
        }
    }

    /**
     * 通过房间ID初始化存在的空房间
     */
    public <RC extends RoomCfg, R extends Room> AbstractRoomController<RC, R> initExistEmptyRoomByRoomId(
            int gameType, int roomCfgId, int maxLimit, long roomId) throws Exception {
        AbstractRoomController<?, ?> roomController = getRoomControllerByRoomId(roomId);
        if (roomController != null) {
            log.error("通过房间ID：{} 重复创建，已存在的房间信息: {}", roomId, roomController.getRoom().logStr());
            // 重复创建
            return null;
        }
        RoomType roomType = RoomType.getRoomType(roomCfgId);
        // 获取roomDao
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDao(roomType);
        if (roomDao == null) {
            return null;
        }
        // 获取当前节点的所有房间
        R existedRoom = roomDao.getRoom(gameType, roomId);
        if (existedRoom == null) {
            log.error("通过房间ID：{} 获取房间为空, 房间类型：{} 房间配置ID：{}", roomId, roomType, roomCfgId);
            return null;
        }
        // 按道理房间不会有玩家
        if (existedRoom.getRoomPlayers() != null && !existedRoom.getRoomPlayers().isEmpty()) {
            log.error("初始化新房间中,有玩家：{}",
                    existedRoom.getRoomPlayers().keySet().stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        return initWithRoom(gameType, roomCfgId, maxLimit, roomType, existedRoom);
    }

    /**
     * 创建初始的系统房间,有些游戏开服就会有默认的房间
     */
    public <RC extends RoomCfg, R extends Room> AbstractRoomController<RC, R> createGameDefaultRoom(
            int gameType, int roomCfgId, int maxLimit) throws Exception {
        RoomType roomType = RoomType.getRoomType(roomCfgId);
        // 获取roomDao
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDao(roomCfgId);
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
     * 通过房间数据初始房间
     */
    private <RC extends RoomCfg, R extends Room> AbstractRoomController<RC, R> initWithRoom(
            int gameType, int roomCfgId, int maxLimit, RoomType roomType, R room) throws Exception {
        // 房间配置
        RC roomCfg = getRoomActualCfg(roomCfgId);
        if (roomCfg == null) {
            log.error("节点创建房间，房间类型: {} 房间ID：{} 找不到配置", gameType, roomCfgId);
            return null;
        }
        // 创建房间控制器
        AbstractRoomController<RC, R> roomController = createRoomController(room, roomCfg);
        // 注册房间控制器
        registerRoomController(gameType, room.getId(), roomController);

        log.debug("系统创建房间成功 gameType = {},roomCfgId = {},roomType = {},maxLimit = {}",
                gameType, roomCfgId, roomType, maxLimit);
        return roomController;
    }

    /**
     * FIXME 在房间退出流程中途加入房间时会有异常情况，正常情况不应该出现，出现中途加入的情况可能是闪断造成的，
     * FIXME 在重复加入时，新加入的玩家有可能还有房间在当前节点，也有可能没有在当前节点，如果没有在当前节点且其他节点还未回存结束，
     * FIXME 这里数据又有新的产生，就会出现数据不一致的情况，导致数据回滚问题
     * player加入房间
     */
    public <RC extends RoomCfg, R extends Room> int joinRoom(
            PlayerController playerController, int gameType, int roomCfgId, long roomId) {
        try {
            if (isRoomStopping) {
                return Code.ROOM_STOPPING;
            }
            if (roomId < 1) {
                log.debug("roomId不能小于,加入房间失败 gameType = {},roomId = {} ,playerId = {}", gameType, roomId, playerController.playerId());
                return Code.FAIL;
            }
            // 检查玩家重复加入房间的情况,如果是机器人重复加入直接退出,真人玩家进行兼容处理
            // TODO 断线重连后修改此段逻辑，需要阻止玩家重复加入
            if (!checkCanRepeatJoinRoom(playerController)) {
                return Code.REPEAT_JOIN_ROOM;
            }
            AbstractRoomController<RC, R> roomController = getRoomController(gameType, roomId);
            if (roomController == null) {
                AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDao(roomCfgId);
                //从redis获取房间数据
                R room = roomDao.getRoom(gameType, roomId);
                if (room == null) {
                    log.warn("加入房间失败，该房间不存在 gameType = {},roomId = {},playerId = {}", gameType, roomId,
                            playerController.playerId());
                    return Code.FAIL;
                }
                //如果该房间所在节点不是本节点，就要切换节点
                if (!room.getPath().equals(this.nodeManager.getNodePath())) {
                    log.info("玩家加入房间时，切换到其他房间节点。");
                    return Code.NOT_FOUND;
                }
                RC roomCfg = getRoomActualCfg(roomCfgId);
                if (roomCfg == null) {
                    log.error("房间类型: {} 找不到配置", roomCfgId);
                    return Code.SAMPLE_ERROR;
                }
                //如果是本节点，先创建roomController
                roomController = createRoomController(room, roomCfg);
                registerRoomController(gameType, roomId, roomController);
            }
            boolean isRobot = playerController.getPlayer() instanceof RobotPlayer;
            if (!isRobot) {
                taskManager.loadTaskData(playerController.playerId());
            }
            //roomController不为空，那么room就是在本节点
            AtomicBoolean reconnect = new AtomicBoolean(false);
            CommonResult<R> addResult = roomController.joinRoom(playerController, reconnect);
            if (!addResult.success()) {
                return Code.JOIN_ROOM_FAILED;
            }
            //断线重连加载离线充值数据
            if (reconnect.get()) {
                rechargeService.loadOfflineRecharge(playerController.playerId());
            }
            R room = addResult.data;
            roomController.setRoom(room);
            //加入成功后还需要更新房间信息
            roomController.onJoinRoomSuccessAfter(playerController);
            if (!isRobot) {
                playerController.setPlayer(playerService.doSave(playerController.playerId(), p -> p.setRoomId(roomId)));
                // gamePlayer需要同步更新数据
                GameDataVo<?> gameDataVo = roomController.getGameController().getGameDataVo();
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerController.playerId());
                gamePlayer.setUpdateTime(playerController.getPlayer().getUpdateTime());
                gamePlayer.setRoomId(roomId);
                log.debug("玩家加入房间成功 gameType = {},roomId = {}, playerId = {} 当前金币：{} 房间人数：{}",
                        roomController.getRoom().getRoomCfgId(),
                        roomId,
                        playerController.playerId(),
                        playerController.getPlayer().getGold(),
                        room.getRoomPlayers().size());
                if (reconnect.get()) {
                    roomController.reconnect(playerController);
                }
            }
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("玩家：{} 房间类型：{} 房间ID: {} 加入发生异常", playerController.playerId(), gameType, roomId, e);
        }
        return Code.FAIL;
    }

    /**
     * 检查玩家是否重复加入
     * 1. 如果房间退出流程异常会导致房间不能正常退出
     * 2. 客户端异常导致前端重复发送加入房间
     */
    private boolean checkCanRepeatJoinRoom(PlayerController playerController) {
        return !(playerController.getScene() instanceof AbstractRoomController<?, ?>);
    }

    /**
     * 玩家断线退出房间
     */
    public <RC extends RoomCfg, R extends Room> void disconnectedExitRoom(PlayerController playerController) {
        try {
            if (playerController.roomId() < 1) {
                log.debug("roomId不能小于,掉线退出房间失败 gameType = {},playerId = {}", playerController.getPlayer().getGameType(),
                        playerController.playerId());
                return;
            }
            AbstractRoomController<RC, R> roomController =
                    getRoomController(playerController.getPlayer().getGameType(), playerController.roomId());
            if (roomController == null) {
                log.warn("掉线退出房间失败，该房间不存在 gameType = {},roomId = {},playerId = {}",
                        playerController.getPlayer().getGameType(), playerController.roomId(), playerController.playerId());
                return;
            }
            Map<Long, RoomPlayer> roomPlayers = roomController.getRoom().getRoomPlayers();
            RoomPlayer roomPlayer = roomPlayers.get(playerController.playerId());
            if (Objects.isNull(roomPlayer)) {
                log.warn("掉线退出房间失败，该用户数据不存在 gameType = {},roomId = {},playerId = {}",
                        playerController.getPlayer().getGameType(), playerController.roomId(), playerController.playerId());
                return;
            }
            GamePlayer gamePlayer = roomController.getGameController().getGamePlayer(playerController.playerId());
            if (gamePlayer == null) {
                log.warn("掉线退出房间失败，该用户游戏数据不存在 gameType = {},roomId = {},playerId = {}",
                        playerController.getPlayer().getGameType(), playerController.roomId(), playerController.playerId());
                return;
            }
            // 房间断线逻辑
            roomController.disconnected(playerController);
            //掉线时回存一次
            roomController.getGameController().directlySavePlayerData(gamePlayer, true);
            taskManager.saveTask(playerController.playerId());
            //更新在线状态
            roomController.updateRoomPlayer(playerController.getPlayer().getGameType(), playerController.roomId(), playerController.playerId(),
                    (newRoomPlayer) -> newRoomPlayer.setOnline(false));
        } catch (Exception e) {
            log.error("掉线退出时异常：{}", e.getMessage(), e);
        }
    }

    /**
     * 玩家退出房间
     */
    public int exitRoom(long playerId) {
        if (playerId < 1) {
            log.warn("玩家ID异常,退出房间失败 playerId = {}", playerId);
            return Code.FAIL;
        }
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController = getRoomControllerByPlayer(playerId);
        if (roomController == null) {
            log.warn("退出房间失败，玩家：{} 的房间不存在", playerId);
            return Code.FAIL;
        }
        // 退出房间
        return exitRoom(roomController.getPlayerController(playerId));
    }

    public int exitRoom(PlayerController playerController) {
        return exitRoom(playerController, true);
    }

    /**
     * 玩家退出房间
     */
    public <RC extends RoomCfg, R extends Room> int exitRoom(PlayerController playerController, boolean removeTaskData) {
        try {
            if (playerController.roomId() < 1) {
                log.debug("roomId不能小于,退出房间失败 gameType = {},playerId = {}", playerController.getPlayer().getGameType(),
                        playerController.playerId());
                return Code.FAIL;
            }

            AbstractRoomController<RC, R> roomController =
                    getRoomController(playerController.getPlayer().getGameType(), playerController.roomId());
            if (roomController == null) {
                log.warn("退出房间失败，该房间不存在 gameType = {},roomId = {},playerId = {}",
                        playerController.getPlayer().getGameType(), playerController.roomId(), playerController.playerId());
                return Code.FAIL;
            }
            // 房间控制器退出房间逻辑
            CommonResult<R> roomResult = roomController.onPlayerLeaveRoom(playerController);
            if (!roomResult.success()) {
                return roomResult.code;
            }
            R room = roomResult.data;
            if (playerController.isRobotPlayer()) {
                // 删除机器人数据
                robotService.recycleRobotPlayer(playerController.playerId());
            }
            if (!(room instanceof FriendRoom)) {
                // 退出房间时删除人数
                matchDataDao.changeRoomJoinNum(room.getGameType(), room.getRoomCfgId(), room.getId(), room.getMaxLimit(), -1, 0);
            }
            // TODO 需要检查房间内玩家是否为空，如果为空则需要检查是否需要删除房间，如果房间不能删除则需要添加机器人进入房间
            // 退出房间将当前场景置为空
            playerController.setScene(null);
            // 将玩家房间ID置为0
            if (!playerController.isRobotPlayer()) {
                Player newPlayer = playerService.doSave(playerController.playerId(), p -> p.setRoomId(0));
                if (newPlayer != null) {
                    playerController.setPlayer(newPlayer);
                }
                if (removeTaskData) {
                    taskManager.onExit(playerController.playerId());
                }
                log.debug("玩家退出房间成功 gameCfgId = {},roomId = {},playerId = {},房间人数：{}",
                        room.getRoomCfgId(),
                        room.getId(),
                        playerController.playerId(),
                        room.getRoomPlayers() != null ? room.getRoomPlayers().size() : 0);
            }
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("退出房间时异常：{}", e.getMessage(), e);
        }
        return Code.FAIL;
    }

    /**
     * 获取同类型的房间id
     */
    public long getSameRoomOtherId(long oldRoomId, int gameType, int roomConfigId, int maxLimit) {
        //取本服的
        Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> map = roomControllerMap.get(gameType);
        if (Objects.nonNull(map)) {
            for (Map.Entry<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> entry : map.entrySet()) {
                if (oldRoomId != entry.getKey() && entry.getValue().getRoom().getRoomCfgId() == roomConfigId) {
                    //获取房间id
                    return entry.getKey();
                }
            }
        }
        return matchDataDao.getNewWaitJoinRoomId(gameType, roomConfigId, maxLimit, oldRoomId);
    }

    /**
     * 机器人批量退出房间
     */
    public <RC extends RoomCfg, R extends Room> int robotPlayerExitRoom(List<PlayerController> playerControllers) {
        try {
            Map<AbstractRoomController<RC, R>, List<PlayerController>> needRemovePlayerControllers =
                    new HashMap<>();
            for (PlayerController playerController : playerControllers) {
                if (playerController == null || playerController.roomId() < 1) {
                    continue;
                }
                if (!(playerController.getPlayer() instanceof RobotPlayer)) {
                    continue;
                }
                AbstractRoomController<RC, R> roomController =
                        getRoomController(playerController.getPlayer().getGameType(), playerController.roomId());
                if (roomController == null) {
                    log.warn("机器人退出房间失败，该房间不存在 gameType = {},roomId = {},playerId = {}",
                            playerController.getPlayer().getGameType(), playerController.roomId(),
                            playerController.playerId());
                    continue;
                }
                // 添加需要处理的机器人
                needRemovePlayerControllers.computeIfAbsent(roomController, k -> new ArrayList<>()).add(playerController);
            }
            if (needRemovePlayerControllers.isEmpty()) {
                return Code.SUCCESS;
            }
            for (Map.Entry<AbstractRoomController<RC, R>, List<PlayerController>> entry :
                    needRemovePlayerControllers.entrySet()) {
                AbstractRoomController<RC, R> roomController = entry.getKey();
                CommonResult<R> leaveRes = roomController.onRobotPlayersLeaveRoom(entry.getValue());
                if (!leaveRes.success()) {
                    continue;
                }
                List<Long> robotPlayers = entry.getValue().stream().map(PlayerController::playerId).toList();
                // 删除机器人数据
                robotService.recycleRobotPlayers(robotPlayers);
                // 将playerController的场景置空
                entry.getValue().forEach(playerController -> playerController.setScene(null));
                return Code.SUCCESS;
            }
        } catch (Exception e) {
            log.error("退出房间时异常：{}", e.getMessage(), e);
        }
        return Code.FAIL;
    }

    /**
     * 解散房间，解散时会:
     * 1. 将玩家踢出房间
     * 2. 调用房间控制器的解散房间逻辑
     * 3. 房间控制器会调用游戏控制器内的解散房间逻辑
     * 4. 回存房间数据
     * 5. 移除房间控制器
     * 6. 删除房间数据
     *
     * @param disbandRoomByPlayer 是否是玩家主动解散房间
     */
    public <R extends Room> void disbandRoom(R room, boolean disbandRoomByPlayer, boolean exitNotify) {
        int gameType = room.getGameType();
        long roomId = room.getId();
        if (!roomControllerMap.containsKey(gameType)) {
            log.error("房间解散时，房间管理器不存在游戏类型: {} 的房间控制器集合", gameType);
            return;
        }
        Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllers =
                roomControllerMap.get(gameType);
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController = roomControllers.get(roomId);
        if (roomController == null) {
            log.error("解散放假时，通过房间ID: {} 查找房间管理器不存在", roomId);
            return;
        }
        EGameType eGameType = EGameType.getGameByTypeId(gameType);
        log.info("开始解散房间：{} 房间类型：{} cfgId: {}", roomId, eGameType.getGameDesc(), room.getRoomCfgId());
        List<PlayerController> robotPlayers = new ArrayList<>();
        for (Map.Entry<Long, RoomPlayer> entry : room.getRoomPlayers().entrySet()) {
            if (!entry.getValue().isRobot()) {
                if (exitNotify) {
                    PlayerController playerController = roomController.getPlayerController(entry.getKey());
                    if (playerController == null) {
                        log.error("解散房间时 未获取到playerController id:{}", entry.getKey());
                        continue;
                    }
                    playerController.send(new ResExitGame(Code.SUCCESS));
                }
                // 将玩家踢出房间
                exitRoom(entry.getKey());
            } else {
                // 将机器人踢出房间
                robotPlayers.add(roomController.getPlayerController(entry.getKey()));
            }
        }
        // 将机器人回收
        robotPlayerExitRoom(robotPlayers);
        // 调用解散房间控制器中的逻辑
        roomController.disbandRoom(disbandRoomByPlayer);
        // 移除房间map中的数据
        roomControllers.remove(roomId);
        // 好友房如果没有主动解散不能删除
        if (room instanceof FriendRoom friendRoom && friendRoom.getStatus() != 3 && !nodeManager.nodeConfig.waitClose()) {
            saveFriendRoomDataOnShutdown(friendRoom);
            log.info("关服回存好友房数据：{}", JSON.toJSONString(friendRoom));
        } else {
            // 刪除房间
            deleteRoomFromRedis(room);
        }
    }

    /**
     * 回存好友房数据
     */
    private <R extends FriendRoom> void saveFriendRoomDataOnShutdown(R room) {
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao =
                (AbstractRoomDao<R, ? extends RoomPlayer>) getRoomDao(room.getClass());
        roomDao.doSave(room, new DataSaveCallback<>() {
            @Override
            public void updateData(R dataEntity) {
            }

            @Override
            public boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.removeAllRobotPlayer();
                // 全量回存房间数据
                BeanUtils.copyProperties(room, dataEntity);
                // 需要将房间路径设置为空，避免出现进入维护的节点
                dataEntity.setPath(null);
                dataEntity.setInGaming(false);
                return true;
            }
        });
    }

    /**
     * 从redis中删除房间信息,当房间清除时需要清除所有redis中有引用到该房间信息的地方
     */
    private <R extends Room> void deleteRoomFromRedis(R room) {
        // 从redis中删除房间信息
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao =
                (AbstractRoomDao<R, ? extends RoomPlayer>) getRoomDao(room.getClass());
        Long removedRes = roomDao.removeRoom(room.getGameType(), room.getId(), room.getRoomCfgId());
        if (removedRes != null) {
            log.info("删除房间：{}, 删除：{} ", room.logStr(), removedRes > 0 ? "成功" : "失败");
        }
        // 需要从房间等待列表中删除
        matchDataDao.removeWaitJoinRoomId(room.getGameType(), room.getRoomCfgId(), room.getId());
    }

    /**
     * 服务器关闭时
     */
    public void onServerShutdown() {
        // 优先暂停游戏中的定时器和状态机
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> values : roomControllerMap.values()) {
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : values.values()) {
                // 先暂停房间中的定时器，状态机等
                try {
                    roomController.stopGame();
                } catch (Exception e) {
                    log.error("服务器关闭时停止游戏失败 roomId:{} gameType:{} roomCfgId:{}", roomController.getRoom().getId(),
                            roomController.getRoom().getGameType(), roomController.getRoom().getRoomCfgId(), e);
                }
            }
        }
        // 暂停成功后，所有数据落地再调用房间解散逻辑
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> values : roomControllerMap.values()) {
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : values.values()) {
                // 调用房间的解散逻辑
                try {
                    disbandRoom(roomController.getRoom(), false, true);
                } catch (Exception e) {
                    log.error("服务器关闭时解散游戏失败 roomId:{} gameType:{} roomCfgId:{}", roomController.getRoom().getId(),
                            roomController.getRoom().getGameType(), roomController.getRoom().getRoomCfgId(), e);
                }
            }
        }
        // 最终关闭房间定时器
        roomTimerCenter.close();
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
        this.roomControllerMap
                .computeIfAbsent(gameType, k -> new ConcurrentHashMap<>())
                .putIfAbsent(roomId, roomController);
    }

    /**
     * 通过房间类型创建房间控制器
     */
    protected <RC extends RoomCfg, R extends Room> AbstractRoomController<RC, R> createRoomController(
            Room room, RC roomCfg) throws Exception {
        Class<? extends RoomPlayer> roomPlayerClass = RoomPlayer.class;
        // 通过游戏类型找到对应的房间控制器
        RoomType roomType = RoomType.getRoomType(roomCfg.getId());
        Class<? extends Room> roomDataTypeClass = roomType.getRoomDataType();
        Class<AbstractRoomController<RC, R>> targetRoomControllerClass = null;
        for (Class<? extends AbstractRoomController> controllerClazz : this.roomControllerClazz) {
            // 获取房间控制器上的房间数据类型
            Set<Class<Object>> roomDataClasses = ReflectUtils.getClassSuperActualType(controllerClazz);
            if (roomDataClasses == null) {
                continue;
            }
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
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDao(room.getRoomCfgId());
        if (roomDao != null) {
            roomController.setRoomDao(roomDao);
        } else {
            log.error("房间数据类: {} 未找到对应的Dao", roomType.getRoomDataType().getSimpleName());
        }
        roomController.setRoomCfg(roomCfg);
        roomController.setTimerCenter(this.roomTimerCenter);
        roomController.setRoomManager(this);
        // 调用房间管理器的初始化方法
        roomController.initial(room);
        return roomController;
    }

    public void createAndStartDefaultRoom(
            WarehouseCfg warehouseCfg, String source) throws Exception {
        int gameType = warehouseCfg.getGameID();
        int maxLimit = SampleDataUtils.getRoomMaxLimit(warehouseCfg).getT2();
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController =
                createGameDefaultRoom(gameType, warehouseCfg.getId(), maxLimit);
        if (roomController == null) {
            log.error("{}创建房间失败 gameType:{} roomCfgId:{}", source, gameType, warehouseCfg.getId());
            return;
        }
        EGameType eGameType = EGameType.getGameByTypeId(gameType);
        log.info("{}创建游戏类型：{} 的房间成功！ID：{} RoomCfgId: {}",
                source,
                eGameType == null ? String.valueOf(gameType) : eGameType.getGameDesc(),
                roomController.getRoom().getId(),
                roomController.getRoom().getRoomCfgId());
        try {
            if (roomController.checkRoomCanContinue() && roomController.getGameController().checkRoomCanStart()) {
                roomController.startGame();
                matchDataDao.addWaitJoinRoomId(gameType,
                        roomController.getRoom().getRoomCfgId(),
                        roomController.getRoom().getId(),
                        System.currentTimeMillis());
                log.info("{}游戏启动成功 roomInfo: {}", source, roomController.getRoom().logStr());
                return;
            }
            log.error("{}游戏启动失败 roomInfo: {}", source, roomController.getRoom().logStr());
        } catch (Exception e) {
            log.error("{}启动系统房异常 roomInfo: {}", source, roomController.getRoom().logStr(), e);
        }

        rollbackCreatedDefaultRoom(roomController, source);
    }

    private void rollbackCreatedDefaultRoom(
            AbstractRoomController<? extends RoomCfg, ? extends Room> roomController, String source) {
        try {
            log.info("{}回滚未成功启动的系统房 roomInfo: {}", source, roomController.getRoom().logStr());
            disbandRoom(roomController.getRoom(), false, false);
        } catch (Exception e) {
            log.error("{}回滚未成功启动的系统房异常 roomInfo: {}", source, roomController.getRoom().logStr(), e);
        }
    }

    /**
     * 通过游戏类型ID查找对应的RoomDao
     */
    public <R extends Room> AbstractRoomDao<R, ? extends RoomPlayer> getRoomDao(int roomCfgId) {
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        // 获取房间类型
        RoomType roomType = RoomType.getRoomType(roomCfgId);
        // 房间为null时
        if (roomType == null) {
            log.error("游戏类型：{} 对应的房间类型：{} 找不到对应的RoomType", roomCfgId, warehouseCfg.getGameType());
            return null;
        } else {
            // 获取roomDao
            AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDao(roomType);
            if (roomDao == null) {
                log.error("游戏类型：{} 对应的房间类型：{} 找不到对应的RoomDao", roomCfgId, roomType);
            }
            return roomDao;
        }
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
     * 游戏控制器class
     */
    public Set<Class<? extends AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>>> getGameControllerClazz() {
        return gameControllerClazz;
    }

    public PlayerExecutorGroupDisruptor getProcessorExecutors() {
        return processorExecutors;
    }

    public MatchDataDao getMatchDataDao() {
        return matchDataDao;
    }

    public RobotService getRobotService() {
        return robotService;
    }

    /**
     * 获取房间控制器
     *
     * @param playerId 玩家ID
     * @return 房间控制器
     */
    public AbstractRoomController<? extends RoomCfg, ? extends Room> getRoomControllerByPlayer(long playerId) {
        List<Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>>> roomMapControllers =
                new ArrayList<>(roomControllerMap.values());
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllerMap :
                roomMapControllers) {
            List<AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllers =
                    new ArrayList<>(roomControllerMap.values());
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : roomControllers) {
                if (roomController.getPlayerControllers().containsKey(playerId)) {
                    return roomController;
                }
            }
        }
        return null;
    }

    /**
     * 通过玩家ID获取到玩家对应的GameController
     */
    public AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> getGameControllerByPlayerId(long playerId) {
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController = getRoomControllerByPlayer(playerId);
        if (roomController == null) {
            return null;
        }
        return roomController.getGameController();
    }


    /**
     * 获取房间控制器
     *
     * @param roomId 房间ID
     * @return 房间控制器
     */
    public AbstractRoomController<? extends RoomCfg, ? extends Room> getRoomControllerByRoomId(long roomId) {
        List<Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>>> roomMapControllers =
                new ArrayList<>(roomControllerMap.values());
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllerMap : roomMapControllers) {
            AbstractRoomController<? extends RoomCfg, ? extends Room> controller = roomControllerMap.get(roomId);
            if (controller != null) {
                return controller;
            }
        }
        return null;
    }

    /**
     * 通过房间ID获取到玩家对应的GameController
     */
    public AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> getGameControllerByRoomId(long roomId) {
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController = getRoomControllerByRoomId(roomId);
        if (roomController == null) {
            return null;
        }
        return roomController.getGameController();
    }

    /**
     * 通过游戏类型获取所有的该类型的游戏控制器
     */
    public List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> getGameControllersByGameType(
            EGameType gameType, RoomType roomType) {
        List<AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> gameControllers =
                new ArrayList<>();
        List<Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>>> roomMapControllers =
                new ArrayList<>(roomControllerMap.values());
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllerMap :
                roomMapControllers) {
            List<AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllers =
                    new ArrayList<>(roomControllerMap.values());
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : roomControllers) {
                AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                        roomController.getGameController();
                GameController annotation = gameController.getClass().getAnnotation(GameController.class);
                if (gameController.gameControlType().equals(gameType) && annotation.roomType().equals(roomType)) {
                    gameControllers.add(roomController.getGameController());
                }
            }
        }
        return gameControllers;
    }

    public boolean isRoomStopping() {
        return isRoomStopping;
    }

    public void setRoomStopping(boolean roomStopping) {
        isRoomStopping = roomStopping;
    }

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(Room_BetCfg.EXCEL_NAME, this::reloadRoomCfgRef)
                .addChangeSampleFileObserveWithCallBack(Room_ChessCfg.EXCEL_NAME, this::reloadRoomCfgRef);
    }

    /**
     * 更新房间中的配置引用
     */
    private void reloadRoomCfgRef() {
        List<AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllers =
                roomControllerMap.values().stream().map(Map::values)
                        .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
        for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : roomControllers) {
            roomController.reloadRoomCfg();
        }
        log.info("更新房间控制器中的配置引用成功！数量：{}", roomControllers.size());
    }

    @Override
    public void onTimer(TimerEvent<IProcessorHandler> event) {
        if (event == null || event.getParameter() == null) {
            return;
        }
        try {
            // 执行事件的回调
            event.getParameter().action();
        } catch (Exception ex) {
            log.error("房间管理器的定时器更新逻辑异常, {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * 空房间检测, 5s 检测一次
     */
    protected void emptyRoomCheck() {
        // 房间配置 <=> Pair<此类房间至少保存的数量，房间删除时间>
        Map<Integer, Pair<Integer, Integer>> roomCfgIdPlayerLimitMap = getRoomPlayerLimitCfg();
        // 房间CfgID <=> 房间真人数量+房间控制器
        Map<Integer, List<Pair<Integer, AbstractRoomController<? extends RoomCfg, ? extends Room>>>> roomNumMap =
                new HashMap<>();
        // 检查空房间
        for (Map.Entry<Integer, Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>>> entry :
                this.roomControllerMap.entrySet()) {
            for (Map.Entry<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllerEntry :
                    entry.getValue().entrySet()) {
                AbstractRoomController<? extends RoomCfg, ? extends Room> roomController =
                        roomControllerEntry.getValue();
                Room room = roomController.getRoom();
                roomNumMap.computeIfAbsent(room.getRoomCfgId(), k -> new ArrayList<>()).add(new Pair<>(room.countPlayers(), roomController));
            }
        }
        log.debug("开始检查空房间：{}", roomNumMap.size());
        // 当前时间
        long curTime = System.currentTimeMillis();
        // 需要销毁的房间
        List<AbstractRoomController<? extends RoomCfg, ? extends Room>> needDestroyRooms = new ArrayList<>();
        // 标记或销毁空的房间
        for (Map.Entry<Integer, List<Pair<Integer, AbstractRoomController<? extends RoomCfg, ? extends Room>>>> entry : roomNumMap.entrySet()) {
            Pair<Integer, Integer> configPair = roomCfgIdPlayerLimitMap.get(entry.getKey());
            if (configPair == null) {
                continue;
            }
            int keepNum = configPair.getFirst();
            // 如果小于最小保存的数量
            if (entry.getValue().size() <= keepNum) {
                continue;
            }
            // 需要过滤掉其他 空房间,按真人数量降序排序
            List<Pair<Integer, AbstractRoomController<? extends RoomCfg, ? extends Room>>> sortedRooms =
                    entry.getValue().stream().sorted((o1, o2) -> Integer.compare(o2.getFirst(), o1.getFirst())).toList();
            // 获取所有的空房间
            List<Pair<Integer, AbstractRoomController<? extends RoomCfg, ? extends Room>>> emptyRoom =
                    sortedRooms.subList(keepNum, sortedRooms.size()).stream()
                            .filter(p -> {
                                if (p.getSecond() instanceof AbstractFriendRoomController<?, ?> friendRoomController) {
                                    // 好友房必须要处于销毁状态且没有玩家才能删除
                                    return friendRoomController.getRoom().getStatus() == 3 &&
                                            p.getSecond().getRoom().countPlayers() <= 0;
                                } else {
                                    // 需要筛选所有空的房间，还有真人的房间不能销毁
                                    return p.getSecond().getRoom().countPlayers() <= 0;
                                }
                            })
                            .toList();
            if (emptyRoom.isEmpty()) {
                continue;
            }
            // 删除时间
            int deleteTime = configPair.getSecond();
            // 立刻销毁
            if (deleteTime <= 0) {
                needDestroyRooms.addAll(emptyRoom.stream().map(Pair::getSecond).toList());
            } else {
                // 标记销毁
                for (Pair<Integer, AbstractRoomController<? extends RoomCfg, ? extends Room>> controllerPair :
                        emptyRoom) {
                    GameDataVo<?> gameDataVo = controllerPair.getSecond().getGameController().getGameDataVo();
                    long roomDestroyTime = gameDataVo.getRoomDestroyTime();
                    if (roomDestroyTime <= 0) {
                        gameDataVo.setRoomDestroyTime(curTime + ((long) deleteTime * TimeHelper.ONE_SECOND_OF_MILLIS));
                    } else if (roomDestroyTime <= curTime) {
                        // 如果到了销毁时间
                        needDestroyRooms.add(controllerPair.getSecond());
                    }
                }
            }
        }
        if (!needDestroyRooms.isEmpty()) {
            // 销毁过期房间
            destroyRoomNow(needDestroyRooms);
        }
        // 当前节点系统房间不足时补齐
        checkAndCreateMissingRooms(roomCfgIdPlayerLimitMap, roomNumMap);
    }

    /**
     * 销毁房间
     *
     * @param needDestroyRooms 需要销毁的房间列表
     */
    protected void destroyRoomNow(List<AbstractRoomController<? extends RoomCfg, ? extends Room>> needDestroyRooms) {
        for (AbstractRoomController<? extends RoomCfg, ? extends Room> needDestroyRoomController : needDestroyRooms) {
            log.info("开始销毁空房间: {}", needDestroyRoomController.getRoom().logStr());
            // 需要将销毁逻辑切换到原来的房间线程执行
            processorExecutors.tryPublish(needDestroyRoomController.getRoom().getId(), 0, new BaseHandler<>() {
                @Override
                public void action() {
                    // 调用房间销毁逻辑
                    needDestroyRoomController.gameDestroy(false, false);
                }
            });
        }
    }

    /**
     * 检查当前节点系统房间是否低于保底数量，低于则补房
     */
    private void checkAndCreateMissingRooms(
            Map<Integer, Pair<Integer, Integer>> roomCfgIdPlayerLimitMap,
            Map<Integer, List<Pair<Integer, AbstractRoomController<? extends RoomCfg, ? extends Room>>>> roomNumMap) {
        if (isRoomStopping || nodeManager.nodeConfig.waitClose()) {
            return;
        }
        Set<Integer> availableGameTypeIds = getEnabledGameTypeIds();
        for (WarehouseCfg warehouseCfg : GameDataManager.getWarehouseCfgList()) {
            if (warehouseCfg.getRoomType() >= GameConstant.RoomTypeCons.FRIEND_ROOM_TYPE_START) {
                continue;
            }
            if (!availableGameTypeIds.contains(warehouseCfg.getGameID())) {
                continue;
            }
            Pair<Integer, Integer> configPair = roomCfgIdPlayerLimitMap.get(warehouseCfg.getId());
            if (configPair == null) {
                continue;
            }
            int keepNum = configPair.getFirst();
            if (keepNum <= 0) {
                continue;
            }
            int currentRoomNum = roomNumMap.getOrDefault(warehouseCfg.getId(), Collections.emptyList()).size();
            if (currentRoomNum < keepNum) {
                AbstractRoomDao<? extends Room, ? extends RoomPlayer> roomDao = getRoomDao(warehouseCfg.getId());
                if (roomDao == null) {
                    continue;
                }
                currentRoomNum = roomDao.getCurrentNodeRoom(warehouseCfg.getGameID(), warehouseCfg.getId()).size();
            }
            if (currentRoomNum >= keepNum) {
                continue;
            }
            int needCreateNum = keepNum - currentRoomNum;
            log.info("空房间检测发现房间不足，开始补房 gameType:{} roomCfgId:{} current:{} keep:{}",
                    warehouseCfg.getGameID(), warehouseCfg.getId(), currentRoomNum, keepNum);
            for (int i = 0; i < needCreateNum; i++) {
                try {
                    createAndStartDefaultRoom(warehouseCfg, "空房间检测");
                } catch (Exception e) {
                    log.error("空房间检测补房异常 gameType:{} roomCfgId:{} index:{}",
                            warehouseCfg.getGameID(), warehouseCfg.getId(), i, e);
                }
            }
        }
    }

    private Set<Integer> getEnabledGameTypeIds() {
        if (gameControllerClazz == null || gameControllerClazz.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> enabledGameTypeIds = gameControllerClazz.stream()
                .map(clazz -> clazz.getAnnotation(GameController.class))
                .filter(Objects::nonNull)
                .map(GameController::gameType)
                .map(EGameType::getGameTypeId)
                .collect(Collectors.toSet());
        int[] needBootGameIds = nodeManager.nodeConfig.getNeedBootGameId();
        if (needBootGameIds == null) {
            return enabledGameTypeIds;
        }
        Set<Integer> needBootGameIdSet = Arrays.stream(needBootGameIds).boxed().collect(Collectors.toSet());
        enabledGameTypeIds.removeIf(gameTypeId -> !needBootGameIdSet.contains(gameTypeId));
        return enabledGameTypeIds;
    }

    /**
     * 获取房间玩家限制配置
     */
    private Map<Integer, Pair<Integer, Integer>> getRoomPlayerLimitCfg() {
        // 房间配置 <=> Pair<此类房间至少保存的数量，房间删除时间>
        return GameDataManager.getWarehouseCfgList().stream().collect(HashMap::new, (map, cfg) -> {
            List<Integer> roomDeletionSolution = cfg.getRoomDeletion_Solution();
            if (roomDeletionSolution.isEmpty()) {
                map.put(cfg.getId(), new Pair<>(0, 0));
            } else if (roomDeletionSolution.size() == 1) {
                map.put(cfg.getId(), new Pair<>(roomDeletionSolution.getFirst(), 0));
            } else {
                map.put(cfg.getId(), new Pair<>(roomDeletionSolution.getFirst(), roomDeletionSolution.get(1)));
            }
        }, HashMap::putAll);
    }

    /**
     * 换房间
     */
    public boolean changeRoom(PlayerController playerController, Room roomData, int gameType, int roomCfgId, int maxLimit) {
        if (roomData instanceof FriendRoom) {
            return false;
        }
        long oldRoomId = roomData.getId();
        //获取另一个房间id
        long roomOtherId = getSameRoomOtherId(oldRoomId, gameType, roomCfgId, maxLimit);
        if (roomOtherId == 0) {
            return false;
        }
        //退出房间
        int exited = exitRoom(playerController, false);
        if (exited != Code.SUCCESS) {
            log.info("换房间时退出当前房间失败 playerId:{} oldRoomId:{} gameType:{} roomConfigId:{}",
                    oldRoomId, roomOtherId, gameType, roomCfgId);
            return false;
        }
        boolean join = matchDataDao.changeRoomJoinNum(gameType, roomCfgId, roomOtherId, maxLimit, 1, 1);
        if (!join) {
            return false;
        }
        //加入房间
        int joined = joinRoom(playerController, gameType, roomCfgId, roomOtherId);
        return joined == Code.SUCCESS;
    }

    /**
     * 获取playerService
     */
    public CorePlayerService getPlayerService() {
        return playerService;
    }

    /**
     * 获取游戏埋点数据logger
     */
    public RoomDataTrackLogger getGameDataTrackLogger() {
        return roomDataTrackLogger;
    }

    public FriendRoomBillHistoryDao getFriendRoomBillHistoryDao() {
        return friendRoomBillHistoryDao;
    }

    public PlayerPackService getPlayerPackService() {
        return playerPackService;
    }

    public MailService getMailService() {
        return mailService;
    }

    public GameEventManager getGameEventManager() {
        return gameEventManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public CoreLogger getCoreLogger() {
        return coreLogger;
    }
}
