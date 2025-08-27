package com.jjg.game.room.manager;

import com.jjg.game.common.cluster.ClusterProcessorExecutors;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.room.AbstractRoomDao;
import com.jjg.game.core.dao.room.PlayerRoomDataDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.match.MatchDataDao;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.utils.ReflectionTool;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.datatrack.RoomDataTrackLogger;
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
public abstract class AbstractRoomManager implements ApplicationContextAware, ConfigExcelChangeListener {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    // 房间管理器是否处于房间关闭流程中
    private boolean isRoomStopping = false;
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
    @Autowired
    private RoomDataTrackLogger roomDataTrackLogger;
    // context
    protected ApplicationContext applicationContext;
    // 房间计时器(线程池)
    protected RoomTimerCenter roomTimerCenter;
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
        this.roomTimerCenter = new RoomTimerCenter("room-timer", processorExecutors);
        this.roomTimerCenter.start();
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
     * 初始化当前节点已经存在的房间，此为兼容逻辑，按道理在服务器关闭时会清除当前节点所有的房间数据
     */
    public <RC extends RoomCfg, R extends Room> AbstractRoomController<RC, R> initNodeExistRoom(
        int gameType, int roomCfgId, int maxLimit) throws Exception {
        RoomType roomType = RoomType.getRoomType(roomCfgId);
        // 获取roomDao
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDao(roomCfgId);
        // TODO 每个游戏类型都会有对应的Room去处理相应的数据，现在没有先退出
        if (roomDao == null) {
            return null;
        }
        // 获取当前节点的所有房间
        List<R> nodeRoom = roomDao.getCurrentNodeRoom(gameType, roomCfgId);
        if (nodeRoom == null || nodeRoom.isEmpty()) {
            log.info("当前节点：{} 游戏类型：{} 的房间为空", nodeManager.getNodePath(), roomCfgId);
            return null;
        }
        R randomRoom = RandomUtils.randCollection(nodeRoom);
        if (randomRoom == null) {
            return null;
        }
        if (randomRoom.getRoomPlayers() != null && !randomRoom.getRoomPlayers().isEmpty()) {
            // 加载时需要移除所有房间中的机器人
            roomDao.doSave(gameType, randomRoom.getId(), new DataSaveCallback<>() {
                @Override
                public void updateData(Room dataEntity) {
                }

                @Override
                public Boolean updateDataWithRes(Room dataEntity) {
                    dataEntity.removeAllRobotPlayer();
                    return true;
                }
            });
        }
        return initWithRoom(gameType, roomCfgId, maxLimit, roomType, randomRoom);
    }

    /**
     * 通过房间ID初始化存在的房间
     */
    public <RC extends RoomCfg, R extends Room> AbstractRoomController<RC, R> initExistRoomByRoomId(
        int gameType, int roomCfgId, int maxLimit, long roomId) throws Exception {
        RoomType roomType = RoomType.getRoomType(roomCfgId);
        // 获取roomDao
        AbstractRoomDao<R, ? extends RoomPlayer> roomDao = getRoomDao(roomType);
        if (roomDao == null) {
            return null;
        }
        // 获取当前节点的所有房间
        R existedRoom = roomDao.getRoom(gameType, roomCfgId);
        if (existedRoom == null) {
            log.info("通过房间ID：{} 获取房间为空", roomId);
            return null;
        }

        // 按道理新房间不会有玩家
        if (existedRoom.getRoomPlayers() != null && !existedRoom.getRoomPlayers().isEmpty()) {
            log.error("初始化新房间中,有玩家：{}",
                existedRoom.getRoomPlayers().keySet().stream().map(String::valueOf).collect(Collectors.joining(",")));
            // 加载时需要移除所有房间中的机器人
            roomDao.doSave(gameType, existedRoom.getId(), new DataSaveCallback<>() {
                @Override
                public void updateData(Room dataEntity) {
                }

                @Override
                public Boolean updateDataWithRes(Room dataEntity) {
                    dataEntity.removeAllRobotPlayer();
                    return true;
                }
            });
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
                log.debug("roomId不能小于,加入房间失败 gameType = {},roomId = {} ,playerId = {}", gameType, roomId,
                    playerController.playerId());
                return Code.FAIL;
            }
            // 检查玩家重复加入房间的情况,如果是机器人重复加入直接退出,真人玩家进行兼容处理
            // TODO 断线重连后修改此段逻辑，需要阻止玩家重复加入
            /*if (!checkCanRepeatJoinRoom(playerController)) {
                return Code.REPEAT_JOIN_ROOM;
            }*/
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
                    MarsNode node = clusterSystem.getNode(room.getPath());
                    if (node == null) {
                        log.warn("加入房间成功，开始切换节点 gameType = {},roomId = {},playerId = {},toRoomPath = {}", gameType,
                            roomId, playerController.playerId(), room.getPath());
                        return Code.FAIL;
                    }
                    clusterSystem.switchNode(playerController.getSession(), node);
                    return Code.SUCCESS;
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

            //roomController不为空，那么room就是在本节点
            CommonResult<R> addResult = roomController.joinRoom(playerController);
            if (!addResult.success()) {
                return Code.JOIN_ROOM_FAILED;
            }
            R room = addResult.data;
            roomController.setRoom(room);
            boolean isRobot = playerController.getPlayer() instanceof RobotPlayer;
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
        List<Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>>> roomMapControllers =
            new ArrayList<>(roomControllerMap.values());
        long playerId = playerController.playerId();
        List<AbstractRoomController<? extends RoomCfg, ? extends Room>> playerRoomControllers = new ArrayList<>();
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllerMap :
            roomMapControllers) {
            List<AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllers =
                new ArrayList<>(roomControllerMap.values());
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : roomControllers) {
                if (roomController.getPlayerControllers().containsKey(playerId)) {
                    playerRoomControllers.add(roomController);
                }
            }
        }
        // 如果是机器人重复加入的情况直接返回，机器人不能重复加入房间，按理不应出现此情况，除非机器人退出失败
        if (!playerRoomControllers.isEmpty() && playerController.isRobotPlayer()) {
            return false;
        }
        // 如果玩家还存在房间中，先执行退出逻辑再进入，TODO 如果后续是断线重连进入则需要进入断线重连逻辑
        // 需要保证一个玩家同时只能在一个房间中
        if (!playerRoomControllers.isEmpty()) {
            List<Room> leaveFailedRoom = new ArrayList<>();
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : playerRoomControllers) {
                CommonResult<? extends Room> leaveRes = roomController.onPlayerLeaveRoom(playerController);
                if (!leaveRes.success()) {
                    if (leaveRes.data != null) {
                        leaveFailedRoom.add(leaveRes.data);
                    }
                } else {
                    log.info("处理玩家重复加入房间，玩家: {} 离开房间: {} 成功", playerId, leaveRes.data.logStr());
                }
            }
            if (!leaveFailedRoom.isEmpty()) {
                log.error("处理玩家重复加入房间时，玩家：{} 离开房间时失败：{}",
                    playerId, leaveFailedRoom.stream().map(Room::logStr).collect(Collectors.joining(",")));
            }
        }
        return true;
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
            // TODO 需要检查房间内玩家是否为空，如果为空则需要检查是否需要删除房间，如果房间不能删除则需要添加机器人进入房间
            // 退出房间将当前场景置为空
            playerController.setScene(null);
            // 将玩家房间ID置为0
            playerService.doSave(playerController.playerId(), p -> p.setRoomId(0));
            boolean isRobot = playerController.getPlayer() instanceof RobotPlayer;
            if (!isRobot) {
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
    public long getSameRoomOtherId(long oldRoomId, int gameType, int roomConfigId) {
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
        return matchDataDao.getNewWaitJoinRoomId(gameType, roomConfigId, oldRoomId);
    }


    /**
     * 机器人批量退出房间
     */
    public <RC extends RoomCfg, R extends Room> int robotPlayerExitRoom(List<PlayerController> playerControllers) {
        try {
            Map<AbstractRoomController<RC, R>, List<PlayerController>> needRemovePlayerControllers =
                new HashMap<>();
            for (PlayerController playerController : playerControllers) {
                if (playerController.roomId() < 1) {
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
     * 1. 调用房间控制器的解散房间逻辑，通知房间内玩家退出房间
     * 2. 房间控制器会调用游戏控制器内的解散房间逻辑
     * 3. 回存房间数据
     * 4. 移除房间控制器
     * 5. 删除房间数据
     */
    public <R extends Room> void disbandRoom(R room) {
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
        // 调用解散房间控制器中的逻辑
        roomController.disbandRoom();
        // TODO 回存房间相关数据

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
                roomController.stopGame();
            }
        }
        // 暂停成功后，所有数据落地再调用房间解散逻辑
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> values : roomControllerMap.values()) {
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : values.values()) {
                // 调用房间的解散逻辑
                disbandRoom(roomController.getRoom());
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
            .computeIfAbsent(roomId,
                k -> roomController);
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
        roomController.initial();
        return roomController;
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
        for (Map<Long, AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllerMap :
            roomMapControllers) {
            List<AbstractRoomController<? extends RoomCfg, ? extends Room>> roomControllers =
                new ArrayList<>(roomControllerMap.values());
            for (AbstractRoomController<? extends RoomCfg, ? extends Room> roomController : roomControllers) {
                if (roomController.getRoom().getId() == roomId) {
                    return roomController;
                }
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
}
