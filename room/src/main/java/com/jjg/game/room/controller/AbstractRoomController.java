package com.jjg.game.room.controller;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.concurrent.PlayerWorker;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.dao.room.AbstractRoomDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.room.base.ERoomState;
import com.jjg.game.room.constant.RoomConstant;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayFlowPojo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.manager.AbstractRoomManager;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.services.RobotService;
import com.jjg.game.room.timer.RoomTimerCenter;
import com.jjg.game.sampledata.bean.RoomCfg;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 房间控制器抽象类,用于控制某一类型的房间
 *
 * @author 11
 * @date 2025/6/25 12:33
 */
public abstract class AbstractRoomController<RC extends RoomCfg, R extends Room> implements TimerListener<IProcessorHandler>,
        IRoomLifeCycle {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    //房间对象
    protected R room;
    //当前房间的玩家
    protected Map<Long, PlayerController> playerControllers = new ConcurrentHashMap<>();
    protected Class<? extends RoomPlayer> roomPlayerClazz;
    protected AbstractRoomDao<R, ? extends RoomPlayer> roomDao;
    // 房间管理器
    protected AbstractRoomManager roomManager;
    // 对局流水
    protected GamePlayFlowPojo gamePlayFlowPojo;
    // 房间计时器，不要将此引用暴露到外部
    protected RoomTimerCenter timerCenter;
    // 房间级周期定时任务句柄，房间销毁时必须取消
    private volatile Timeout checkNoJoinPlayerTimeout;
    private volatile Timeout roomTickTimeout;
    // 游戏控制器
    protected AbstractGameController<RC, ? extends GameDataVo<RC>> gameController;
    // 房间配置
    protected RC roomCfg;
    // 机器人上次创建的时间
    protected long robotLastCreatedTime;
    // 房间状态
    private volatile ERoomState roomState;
    // 游戏事件管理器
    private GameEventManager gameEventManager;
    // 房间数据差异计数
    private int diffCount = 0;
    /**
     * 任务管理器
     */
    private TaskManager taskManager;

    public AbstractRoomController(Class<? extends RoomPlayer> roomPlayerClazz, R room) {
        this.roomPlayerClazz = roomPlayerClazz;
        this.room = room;
    }

    /**
     * 通过配置创建游戏controller
     *
     * @param roomCfg 配置
     */
    public <RD extends GameDataVo<RC>> AbstractGameController<RC, RD> createGameController(RC roomCfg) {
        Set<Class<? extends AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>>> gameControllerClazz =
                roomManager.getGameControllerClazz();
        try {
            for (Class<? extends AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> controllerClazz :
                    gameControllerClazz) {
                GameController gameAnnotateController = controllerClazz.getAnnotation(GameController.class);
                EGameType games = gameAnnotateController.gameType();
                RoomType roomType = RoomType.getRoomType(roomCfg.getId());
                boolean isEqualsRoomType = roomType.equals(gameAnnotateController.roomType());
                // 游戏类型和房间类型必须一致
                if (games.getGameTypeId() == roomCfg.getGameID() && isEqualsRoomType) {
                    Constructor<AbstractGameController<RC, RD>> constructor =
                            (Constructor<AbstractGameController<RC, RD>>) controllerClazz.getDeclaredConstructor(AbstractRoomController.class);
                    // 将调用当前方法的RoomController写入GameController中
                    AbstractGameController<RC, RD> gameController = constructor.newInstance(this);
                    RD roomDataVoCopied = gameController.createRoomDataVo(roomCfg);
                    roomDataVoCopied.setRoomId(room.getId());
                    gameController.setGameDataVo(roomDataVoCopied);
                    gameController.initTimerCenter(timerCenter);
                    gameController.initial(room);
                    return gameController;
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            log.error("创建游戏控制器失败：{} err: {}", room.logStr(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * 玩家加入房间
     *
     * @param reconnect 重连标识
     */
    public CommonResult<R> joinRoom(PlayerController playerController, AtomicBoolean reconnect) {
        CommonResult<R> result = new CommonResult<>(Code.SUCCESS);
        try {
            //检查玩家在不在房间
            RoomPlayer roomPlayer = room.getPlayer(playerController.playerId());
            //如果不在房间
            if (roomPlayer == null) {
                // 检查加入逻辑
                CommonResult<R> doResult = checkRoomCanJoin(playerController);
                if (!doResult.success()) {
                    if (!playerController.isRobotPlayer()) {
                        log.warn("加入房间失败 gameType = {},roomId = {},playerId = {} 加入结果：{}",
                                playerController.getPlayer().getGameType(),
                                this.room.getId(),
                                playerController.playerId(),
                                doResult.code);
                    }
                    result.code = doResult.code;
                    return result;
                }
                this.room = doResult.data;
                // 此时游戏可能还未开始，需要游戏判断加入时逻辑
            } else if (!roomPlayer.isOnline()) {
                updateRoomPlayer(room.getGameType(), room.getId(), playerController.playerId(), (newRoomPlayer) -> newRoomPlayer.setOnline(true));
                reconnect.set(true);
            } else {
                log.error("玩家已经在房间中 roomId = {},playerId = {}", room.getId(), playerController.playerId());
            }
            gameController.onPlayerJoinRoom(playerController, reconnect);
            playerControllers.put(playerController.playerId(), playerController);
            playerController.setScene(this);
            //非重连
            if (!reconnect.get()) {
                // 当玩家加入时尝试开启游戏
                tryStartGameOnPlayerJoinIn(playerController);
                if (!(playerController.getPlayer() instanceof RobotPlayer) && !(room instanceof FriendRoom)) {
                    boolean deduction = roomManager.getMatchDataDao().changeRoomJoinNum(room.getGameType(), room.getRoomCfgId(), room.getId(), room.getMaxLimit(), 0,
                            -1, room.getPath());
                    if (!deduction) {
                        log.error("房间减少等待人数失败 roomId = {},playerId = {}", room.getId(), playerController.playerId());
                    }
                }
            }
            result.data = room;
            return result;
        } catch (Exception e) {
            log.error("加入房间时出现异常", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 尝试开启游戏
     */
    protected void tryStartGameOnPlayerJoinIn(PlayerController playerController) {
        boolean checkRoomCanContinue = checkRoomCanContinue();
        if (!playerController.isRobotPlayer()) {
            log.info("尝试启动游戏：玩家：{} 房间是否可以开始：{} 游戏是否可以开始：{} 游戏当前状态：{}",
                    playerController.playerId(),
                    checkRoomCanContinue,
                    gameController.checkRoomCanStart(),
                    gameController.getGameState()
            );
        }
        // 检查房间开始的逻辑，由房间判断和游戏判断开启时机
        if (checkRoomCanContinue && gameController.checkRoomCanStart()) {
            // 检查通过开始游戏
            startGame();
        }
    }


    /**
     * 更新房间玩家信息
     *
     * @param gameType       游戏类型
     * @param roomId         房间id
     * @param playerId       玩家id
     * @param updateFunction 更新函数
     */
    public void updateRoomPlayer(int gameType, long roomId, long playerId, Consumer<RoomPlayer> updateFunction) {
        R r = roomDao.updateRoomPlayer(gameType, roomId, playerId, updateFunction);
        if (r != null) {
            setRoom(r);
        }
    }

    /**
     * 更新房间内的座位信息
     *
     * @param gamePlayer     游戏玩家数据
     * @param newSitIndex    新座位id
     * @param forcedExchange 是否强制交换
     */
    public boolean updateRoomPlayerSitInfo(GamePlayer gamePlayer, int newSitIndex, boolean forcedExchange) {
        R r = roomDao.updateRoomPlayerSitInfo(gamePlayer.getGameType(), gamePlayer.getRoomId(), gamePlayer.getId(), newSitIndex, forcedExchange);
        if (r != null) {
            setRoom(r);
            return true;
        }
        return false;
    }

    /**
     * 检查房间的加入逻辑
     *
     * @param playerController 玩家控制器
     */
    protected CommonResult<R> checkRoomCanJoin(PlayerController playerController) {
        R thatRoom = this.room;
        // 房间处于销毁流程中，不能加入
        if (roomState == ERoomState.ROOM_DESTROYING || roomState == ERoomState.ROOM_DESTROYED) {
            log.debug("玩家：{} 不能加入，当前房间处于销毁状态", playerController.playerId());
            return new CommonResult<>(Code.FORBID);
        }
        return roomDao.doSave(playerController.getPlayer().getGameType(),
                this.room.getId()
                , new DataSaveCallback<>() {
                    @Override
                    public void updateData(Room dataEntity) {
                    }

                    @Override
                    public boolean updateDataWithRes(Room room) {
                        try {
                            if (room == null) {
                                log.error("检查房间：{} 是否可以加入时, 房间为空，player: {}",
                                        thatRoom.logStr(), playerController.playerId());
                                return false;
                            }
                            //先检查该玩家是否已经在该房间中
                            boolean exist = room.hasPlayer(playerController.playerId());
                            if (exist) {
                                log.debug("玩家已在房间中 gameType = {},roomId = {},playerId = {}",
                                        playerController.getPlayer().getGameType(), thatRoom.getId(),
                                        playerController.playerId());
                                return true;
                            }

                            if (!room.canEnter()) {
                                if (!playerController.isRobotPlayer()) {
                                    log.debug("该房间无法进入 gameType = {},roomId = {},playerId = {} roomPlayerSize: {} " +
                                                    "roomLimit: {}",
                                            playerController.getPlayer().getGameType(), thatRoom.getId(),
                                            playerController.playerId(),
                                            AbstractRoomController.this.room.getRoomPlayers().size(),
                                            AbstractRoomController.this.room.getMaxLimit());
                                }
                                return false;
                            }

                            //如果之前不在房间，则按座位排座
                            for (int i = 0; i < room.getMaxLimit(); i++) {
                                boolean flag = room.setHasPlayer(i);
                                if (!flag) {
                                    RoomPlayer tmpRoomPlayer = roomDao.createRoomPlayer(playerController);
                                    tmpRoomPlayer.setSit(i);
                                    tmpRoomPlayer.setOnline(true);
                                    tmpRoomPlayer.setRobot(playerController.getPlayer() instanceof RobotPlayer);
                                    room.addPlayer(tmpRoomPlayer);
                                    return true;
                                }
                            }
                        } catch (Exception e) {
                            log.error("检查房间是否可以加入时发生异常", e);
                        }
                        return false;
                    }
                });
    }

    /**
     * 直接，开始游戏，不等待玩家
     */
    @Override
    public void startGame() {
        // 调用游戏本身的初始化逻辑
        gameController.initialGame();
        // 调用子类的启动方法
        gameController.startGame();
        // 修改状态
        roomState = ERoomState.GAMING;
    }

    @Override
    public void onTimer(TimerEvent<IProcessorHandler> e) {
        if (e.getParameter() != null) {
            try {
                e.getParameter().action();
            } catch (Exception ex) {
                log.error("执行房间：{} 游戏类型：{} 定时器异常, msg: {} ",
                        room.getId(), room.getGameType(), ex.getMessage(), ex);
            }
        }
    }

    /**
     * 房间游戏结束
     */
    @Override
    public void gameDestroy(boolean closeByPlayer, boolean notifyExit) {
        cancelWheelTimers();
        // 调用游戏控制器中的结束逻辑
        gameController.gameDestroy(closeByPlayer, notifyExit);
    }

    @Override
    public void pauseGame() {
        roomState = ERoomState.PAUSING;
        gameController.pauseGame();
    }

    public void pausedGame() {
        roomState = ERoomState.PAUSED;
    }

    @Override
    public boolean tryContinueGame() {
        if (gameController.tryContinueGame()) {
            roomState = ERoomState.GAMING;
            return true;
        }
        return false;
    }

    @Override
    public void disbandRoom(Boolean disbandRoomByPlayer) {
        cancelWheelTimers();
        // 调用房间控制器中的解散房间逻辑
        gameController.disbandRoom(disbandRoomByPlayer);
        // 标记游戏状态为销毁完成
        gameController.markDestroyed();
        roomState = ERoomState.ROOM_DESTROYED;
        // 向玩家发送解散房间消息
        broadcastDisbandRoomMsg();
        // 时间管理移除当前注册器
        timerCenter.remove(this);
    }

    /**
     * 广播房间解散消息
     */
    private void broadcastDisbandRoomMsg() {
        // TODO 房间解散
    }

    /**
     * 玩家被动进入下线情况,断线逻辑
     */
    public void playerOffline(PlayerController playerController) {
        playerControllers.remove(playerController.playerId());
    }

    /**
     * 初始化房间控制器逻辑，添加定时逻辑、初始化线程...
     */
    @Override
    public <G extends Room> void initial(G room) {
        roomState = ERoomState.INIT_START;
        // 游戏事件管理器
        gameEventManager = roomManager.getGameEventManager();
        taskManager = roomManager.getTaskManager();
        // 创建游戏控制器
        gameController = createGameController(roomCfg);
        if (gameController == null) {
            // 没有找到对应的游戏控制器，可能是没有实现对应的游戏控制器，先中断流程
            throw new RuntimeException("游戏类型：" + roomCfg.getId() + " 未实现游戏控制器");
        }
        // 房间Timer执行tick时间 现在默认 200ms
        // 添加房间tick
        //机器人检查
        cancelWheelTimers();
        checkNoJoinPlayerTimeout = WheelTimerUtil.scheduleAtFixedRate(this::checkNoJoinPlayer, RoomConstant.ROOM_TICK_TIME, RoomConstant.ROOM_PLAYER_NUM_CHECK, TimeUnit.MILLISECONDS);
        roomTickTimeout = WheelTimerUtil.scheduleAtFixedRate(this::roomTick, RoomConstant.ROOM_TICK_TIME, RoomConstant.ROOM_TICK_TIME, TimeUnit.MILLISECONDS);
        roomState = ERoomState.READY;
    }

    /**
     * 检查未加入人数
     */
    public void checkNoJoinPlayer() {
        PlayerWorker baseFuncProcessor = getRoomProcessor();
        baseFuncProcessor.tryPublish(0, new BaseHandler<String>() {
            @Override
            public void action() {
                RC cfg = gameController.getGameDataVo().getRoomCfg();
                diffCount = roomManager.getMatchDataDao().checkPlayerExpiredWaitingNum(diffCount, cfg.getGameID(), cfg.getId(), room);
            }
        }.setHandlerParamWithSelf("checkNoJoinPlayer"));

    }


    @Override
    public void roomReady() {

    }

    @Override
    public void roomTick() {
        if (isStoping()) {
            return;
        }
        // 游戏启动后进行tick
        if (gameController.isGameStarted()) {
            // 由房间控制器调用game的tick统一控制
            gameController.roomTick();
            // 机器人加入逻辑
            checkRobotJoinRoom();
        }
    }


    protected boolean checkRobotJoinRoomCondition() {
        return true;
    }

    /**
     * 检查机器人添加逻辑
     */
    protected void checkRobotJoinRoom() {
        PlayerWorker baseFuncProcessor = getRoomProcessor();
        // 必须在房间线程中执行
        baseFuncProcessor.tryPublish(0, new BaseHandler<String>() {
            @Override
            public void action() {
                // 创建人数达到上限
                if (room.getRoomPlayers() != null && room.getRoomPlayers().size() >= room.getMaxLimit() || !checkRobotJoinRoomCondition()) {
                    return;
                }
                long currentTimeMillis = System.currentTimeMillis();
                if (robotLastCreatedTime > currentTimeMillis) {
                    return;
                }
                List<Integer> robotIntervalTime = roomCfg.getIntervalTime();
                int randomTime;
                if (robotIntervalTime == null || robotIntervalTime.size() < 2) {
                    randomTime = 1500;
                } else {
                    // 毫秒
                    randomTime = RandomUtils.randomMinMax(robotIntervalTime.get(0), robotIntervalTime.get(1));
                }
                // 机器人创建时间更新
                robotLastCreatedTime = currentTimeMillis + randomTime;
                int roomCfgId = roomCfg.getId();
                RobotService robotService = roomManager.getRobotService();
                // 如果房间的
                if (!robotService.checkCanCreateRobot(roomCfgId, room)) {
                    return;
                }
                // 创建一个机器人
                PlayerController robotPlayerController = robotService.getOrCreateRobotPlayerController(roomCfgId, room.getId());
                if (robotPlayerController == null) {
                    // 返回
                    return;
                }
                boolean incremented = roomManager.getMatchDataDao().changeRoomJoinNum(room.getGameType(),
                        room.getRoomCfgId(), room.getId(), room.getMaxLimit(), 1, 0, room.getPath());
                if (!incremented) {
                    //释放机器人
                    robotService.recycleRobotPlayers(List.of(robotPlayerController.getPlayer().getId()));
                    log.debug("机器人加入房间失败, 房间已满 {}", room.logStr());
                    return;
                }
                // 将机器人加入房间中
                int code = roomManager.joinRoom(robotPlayerController, room.getGameType(), roomCfgId, room.getId());
                // 如果加入失败则走一次退出房间逻辑
                if (code != Code.SUCCESS) {
                    log.debug("机器人加入房间失败, code : {} {}", code, room.logStr());
                }
            }
        }.setHandlerParamWithSelf("room tick robot join"));
    }

    @Override
    public void hosting() {

    }

    @Override
    public void reconnect(PlayerController playerController) {
        gameController.reconnect(playerController);
    }

    /**
     * 玩家主动退出房间
     */
    @Override
    public CommonResult<R> onPlayerLeaveRoom(PlayerController playerController) {
        CommonResult<R> result = new CommonResult<>(Code.SUCCESS);
        try {
            // 调用游戏的离开房间逻辑
            gameController.onPlayerLeaveRoom(playerController);
            // 移除玩家PlayerController
            playerControllers.remove(playerController.getPlayer().getId());
            //从room中移除
            R room = roomDao.removePlayer(
                    playerController.getPlayer().getGameType(),
                    playerController.roomId(),
                    playerController.playerId());
            if (room != null) {
                log.debug("强制离开房间成功, gameType = {},roomId = {},playerId = {}",
                        playerController.getPlayer().getGameType(),
                        playerController.roomId(),
                        playerController.playerId());
            } else {
                log.debug("将玩家从房间中移除失败 gameType = {},roomId = {},playerId = {}",
                        this.room.getGameType(), this.room.getId(), playerController.playerId());
                result.code = Code.FAIL;
                return result;
            }
            result.data = room;
            this.room = room;
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 机器人离开房间
     */
    public CommonResult<R> onRobotPlayersLeaveRoom(List<PlayerController> playerControllers) {
        CommonResult<R> result = new CommonResult<>(Code.SUCCESS);
        try {
            for (PlayerController playerController : playerControllers) {
                // 调用游戏的离开房间逻辑
                gameController.onPlayerLeaveRoom(playerController);
            }
            // 移除玩家PlayerController
            this.playerControllers.values().removeAll(playerControllers);
            //从room中移除所有玩家
            R room = roomDao.removePlayers(
                    this.room.getGameType(),
                    this.room.getId(),
                    playerControllers.stream().map(PlayerController::playerId).toList());
            if (room == null) {
                log.debug("将玩家从房间中移除失败 gameType = {},roomId = {},playerIds = {}",
                        this.room.getRoomCfgId(),
                        this.room.getId(),
                        playerControllers.stream().map(PlayerController::playerId).map(String::valueOf).collect(Collectors.joining(",")));
                result.code = Code.FAIL;
                return result;
            }
            result.data = room;
            this.room = room;
            // 退出房间时删除人数
            roomManager.getMatchDataDao().changeRoomJoinNum(room.getGameType(), room.getRoomCfgId(), room.getId(), room.getMaxLimit(),
                    -playerControllers.size(), 0, room.getPath());
        } catch (Exception e) {
            log.error("机器人退出房间时异常", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 通过消息builder向房间广播消息。!!!NOTICE!!!: 不要再在RoomController中写其他广播方法
     *
     * @param roomMessageBuilder 消息builder
     */
    public <T extends AbstractMessage> void broadcastToPlayers(RoomMessageBuilder<T> roomMessageBuilder) {
        T message = roomMessageBuilder.getData();
        Set<Long> playerIds = roomMessageBuilder.getPlayerIds();
        if (message == null) {
            log.error("向房间广播消息时，消息为空");
            return;
        }
        // 如果同时需要发送某几个玩家又需要推送所有人，则直接推送给所有人
        if (roomMessageBuilder.isToAll()) {
            // 可能为空 出现在进入房间之前就发房间消息或者在离开房间之后还在发送消息
            playerIds = playerControllers.keySet();
        }
        // 玩家首次创建房间进入时也会出现为空的情况
        if (playerIds.isEmpty()) {
            return;
        }
        Set<Long> exceptPlayerIds = roomMessageBuilder.getExceptPlayers();
        for (Long roomPlayerId : playerIds) {
            RoomPlayer roomPlayer = getRoomPlayer(roomPlayerId);
            if (roomPlayer == null || !roomPlayer.isOnline()) {
                continue;
            }
            // 如果有不需要发送消息的玩家
            if (!exceptPlayerIds.isEmpty() && exceptPlayerIds.contains(roomPlayerId)) {
                continue;
            }
            PlayerController playerController = playerControllers.get(roomPlayerId);
            boolean playerIsOnline = playerController != null;
            if (playerIsOnline) {
                playerIsOnline = playerController.isOnline();
            }

            // 需要玩家在线并且不是机器人，则发送数据
            if (playerIsOnline && !(playerController.getPlayer() instanceof RobotPlayer)) {
                playerController.send(message);
            }
        }
    }

    public R getRoom() {
        return room;
    }

    public void setRoom(R room) {
        this.room = room;
    }

    public Map<Long, PlayerController> getPlayerControllers() {
        return playerControllers;
    }

    public PlayerController getPlayerController(long playerId) {
        return playerControllers.get(playerId);
    }

    public AbstractRoomDao<R, ? extends RoomPlayer> getRoomDao() {
        return roomDao;
    }

    public void setRoomDao(AbstractRoomDao<? extends Room, ? extends RoomPlayer> roomDao) {
        this.roomDao = (AbstractRoomDao<R, ? extends RoomPlayer>) roomDao;
    }

    public GamePlayFlowPojo getGamePlayFlowPojo() {
        return gamePlayFlowPojo;
    }

    public void setGamePlayFlowPojo(GamePlayFlowPojo gamePlayFlowPojo) {
        this.gamePlayFlowPojo = gamePlayFlowPojo;
    }

    public AbstractRoomManager getRoomManager() {
        return roomManager;
    }

    public void setRoomManager(AbstractRoomManager roomManager) {
        this.roomManager = roomManager;
    }

    public void setTimerCenter(RoomTimerCenter timerCenter) {
        this.timerCenter = timerCenter;
    }

    public Class<? extends RoomPlayer> getRoomPlayerClazz() {
        return roomPlayerClazz;
    }

    public AbstractGameController<RC, ? extends GameDataVo<RC>> getGameController() {
        return gameController;
    }

    public PlayerWorker getRoomProcessor() {
        return roomManager.getProcessorExecutors().getPlayerWorker(room.getId());
    }

    public void setGameController(AbstractGameController<RC, ? extends GameDataVo<RC>> gameController) {
        this.gameController = gameController;
    }

    public void setRoomCfg(RC roomCfg) {
        this.roomCfg = roomCfg;
    }

    /**
     * 重新获取房间配置
     */
    public abstract void reloadRoomCfg();

    /**
     * 是否停止游戏
     */
    public boolean isStoping() {
        return roomState == ERoomState.ROOM_DESTROYING || roomState == ERoomState.ROOM_DESTROYED;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRoomController<?, ?> that = (AbstractRoomController<?, ?>) o;
        return Objects.equals(room, that.room) && Objects.equals(roomCfg, that.roomCfg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(room, roomCfg);
    }

    @Override
    public void stopGame() {
        log.info("开始停止游戏 {}", room.logStr());
        roomState = ERoomState.ROOM_DESTROYING;
        cancelWheelTimers();
        // 移除定时器
        this.timerCenter.remove(this);
        // 暂停游戏
        gameController.stopGame();
    }

    private void cancelWheelTimers() {
        Timeout checkTimeout = checkNoJoinPlayerTimeout;
        if (checkTimeout != null && !checkTimeout.isCancelled()) {
            checkTimeout.cancel();
        }
        checkNoJoinPlayerTimeout = null;

        Timeout tickTimeout = roomTickTimeout;
        if (tickTimeout != null && !tickTimeout.isCancelled()) {
            tickTimeout.cancel();
        }
        roomTickTimeout = null;
    }

    public boolean isStartedGame() {
        return gameController.isGameStarted();
    }

    /**
     * 检查房间是否可以继续
     */
    public boolean checkRoomCanContinue() {
        return true;
    }

    /**
     * 当检查房间不能继续时调用
     */
    public void onRoomCantContinue() {

    }

    public ERoomState getRoomState() {
        return roomState;
    }

    public RoomPlayer getRoomPlayer(long playerId) {
        return room.getRoomPlayers().get(playerId);
    }

    public GameEventManager getGameEventManager() {
        return gameEventManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    /**
     * 加入房间成功之后,可以更新房间数据
     */
    public void onJoinRoomSuccessAfter(PlayerController playerController) {
        gameController.onJoinRoomSuccessAfter(playerController);
    }

    public void onFriendRoomCreate() {

    }
}
