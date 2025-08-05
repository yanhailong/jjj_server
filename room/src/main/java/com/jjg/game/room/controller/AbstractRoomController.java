package com.jjg.game.room.controller;

import com.jjg.game.common.concurrent.BaseFuncProcessor;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.dao.PlayerRoomDataDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.constant.RoomConstant;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayFlowPojo;
import com.jjg.game.room.manager.AbstractRoomManager;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.sample.bean.WarehouseCfg;
import com.jjg.game.room.timer.RoomEventType;
import com.jjg.game.room.timer.RoomTimerCenter;
import com.jjg.game.room.timer.RoomTimerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    // 游戏控制器
    protected AbstractGameController<RC, ? extends GameDataVo<RC>> gameController;
    // 房间线程
    protected BaseFuncProcessor roomProcessor;
    // 房间配置
    protected RC roomCfg;
    // 机器人上次创建的时间
    private long robotLastCreatedTime;
    // 是否开始停止逻辑
    private volatile boolean isStoping = false;

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
                if (games.getGameTypeId() == roomCfg.getGameID()) {
                    Constructor<AbstractGameController<RC, RD>> constructor =
                            (Constructor<AbstractGameController<RC, RD>>) controllerClazz.getDeclaredConstructor(AbstractRoomController.class);
                    // 将调用当前方法的RoomController写入GameController中
                    AbstractGameController<RC, RD> gameController = constructor.newInstance(this);
                    RD roomDataVoCopied = gameController.createRoomDataVo(roomCfg);
                    roomDataVoCopied.setRoomId(room.getId());
                    gameController.setGameDataVo(roomDataVoCopied);
                    gameController.initTimerCenter(timerCenter);
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
     */
    public CommonResult<R> joinRoom(PlayerController playerController) {
        CommonResult<R> result = new CommonResult<>(Code.SUCCESS);
        try {
            //检查玩家在不在房间
            RoomPlayer roomPlayer = room.getPlayer(playerController.playerId());
            //如果不在房间
            if (roomPlayer == null) {
                // 检查加入逻辑
                CommonResult<? extends Room> doResult = checkRoomCanJoin(playerController);
                if (!doResult.success()) {
                    if (!playerController.isRobotPlayer()) {
                        log.warn("加入房间失败 gameType = {},roomId = {},playerId = {} 加入结果：{}",
                                playerController.getPlayer().getGameType(),
                                this.room.getId(),
                                playerController.playerId(),
                                doResult.code);
                    }
                    result.code = Code.FAIL;
                    return result;
                }
                this.room = (R) doResult.data;
                // 此时游戏可能还未开始，需要游戏判断加入时逻辑
                gameController.onPlayerJoinRoom(playerController, gameController.isGameStarted());
            } else {
                // TODO 异常流程，按道理不应出现此逻辑。后续处理
                log.debug("玩家已经在房间中 roomId = {},playerId = {}", room.getId(), playerController.playerId());
                gameController.onPlayerJoinRoom(playerController, gameController.isGameStarted());
            }
            playerControllers.put(playerController.playerId(), playerController);
            // 检查等待房间的逻辑
            updateWaitRoomList();
            playerController.setScene(this);
            // 检查房间开始的逻辑，由游戏自行判断开启时机
            if (gameController.checkRoomCanStart()) {
                // 检查通过开始游戏
                startGame();
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
     * 更新房间等待列表
     */
    private void updateWaitRoomList() {
        // 如果房间已满，则需要将等待房间列表从redis中删除
        if (!room.canEnter()) {
            roomManager.getMatchDataDao().removeWaitJoinRoomId(
                    room.getGameType(), room.getRoomCfgId(), room.getId());
        } else {
            // 如果房间未满，则将直接写入等待列表
            roomManager.getMatchDataDao().addWaitJoinRoomId(
                    room.getGameType(), room.getRoomCfgId(), room.getId(), room.getCreateTime());
        }
    }

    /**
     * 检查房间的加入逻辑
     *
     * @param playerController 玩家控制器
     */
    protected CommonResult<? extends Room> checkRoomCanJoin(PlayerController playerController) {
        R thatRoom = this.room;
        return roomDao.doSave(playerController.getPlayer().getGameType(),
                this.room.getId()
                , new DataSaveCallback<>() {
                    @Override
                    public void updateData(Room dataEntity) {
                    }

                    @Override
                    public Boolean updateDataWithRes(Room room) {
                        try {
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
                                    tmpRoomPlayer.setPlayer(playerController.getPlayer());
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
     * 开始游戏
     */
    @Override
    public void startGame() {
        // 调用游戏本身的初始化逻辑
        gameController.initial();
        // 调用子类的启动方法
        gameController.startGame();
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
    public void gameOver() {
        // 调用游戏控制器中的结束逻辑
        gameController.gameOver();
    }

    @Override
    public void disbandRoom() {
        // 调用房间控制器中的解散房间逻辑
        gameController.disbandRoom();
        // 回存房间数据
        saveRoomData();
        // 向玩家发送解散房间消息
        // broadcastDisbandRoomMsg()
        // 时间管理移除当前注册器
        timerCenter.remove(this);
    }

    /**
     * 存储房间数据
     */
    private void saveRoomData() {
        PlayerRoomDataDao playerRoomDataDao = roomManager.getPlayerRoomDataDao();
        List<PlayerRoomData> playerRoomDataList = new ArrayList<>();
        for (Map.Entry<Long, RoomPlayer> entry : room.getRoomPlayers().entrySet()) {
            if (entry.getValue().getPlayer() == null || entry.getValue().whichIsRobot()) {
                continue;
            }
            PlayerRoomData playerRoomData = entry.getValue().getPlayerRoomData();
            if (playerRoomData != null && playerRoomData.getPlayerId() > 0) {
                playerRoomDataList.add(playerRoomData);
            }
        }
        if (!playerRoomDataList.isEmpty()) {
            // 回存玩家房间数据
            playerRoomDataDao.saveAll(playerRoomDataList);
        }
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
    public void initial() {
        // 当前房间的线程实例，用于投递一些异步任务
        roomProcessor = roomManager.getProcessorExecutors().getProcessorById(room.getId());
        // 创建游戏控制器
        gameController = createGameController(roomCfg);
        if (gameController == null) {
            // 没有找到对应的游戏控制器，可能是没有实现对应的游戏控制器，先中断流程
            throw new RuntimeException("游戏类型：" + roomCfg.getId() + " 未实现游戏控制器");
        }
        // 空房间检查
        addEmptyRoomCheckTimer();
        // 房间Timer执行tick时间 现在默认 100ms
        // 添加房间tick
        timerCenter.add(new RoomTimerEvent<>(
                this, room, this::timeTick, RoomConstant.ROOM_TICK_TIME, RoomEventType.ROOM_PHASE_RUN_EVENT));
    }

    /**
     * 子类不需要处理此逻辑
     */
    private void addEmptyRoomCheckTimer() {
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfg.getId());
        List<Integer> roomDeletionSolution = warehouseCfg.getRoomDeletion_Solution();
        if (roomDeletionSolution == null || roomDeletionSolution.size() < 2) {
            return;
        }
        int deleteTimeCheck = roomDeletionSolution.get(1);
        timerCenter.add(new RoomTimerEvent<>(this, room, deleteTimeCheck, () -> {

        }, RoomEventType.ROOM_EMPTY_ROOM_CHECK));
    }

    @Override
    public void roomReady() {

    }

    @Override
    public void timeTick() {
        if (isStoping) {
            return;
        }
        // 游戏启动后进行tick
        if (gameController.isGameStarted()) {
            // 由房间控制器调用game的tick统一控制
            gameController.timeTick();
        }
        // 机器人加入逻辑
        checkAddRobot();
    }

    /**
     * 换房间
     */
    public boolean changeRoom(PlayerController playerController, long oldRoomId, int gameType, int roomConfigId) {
        //获取另一个房间id
        long roomOtherId = roomManager.getSameRoomOtherId(oldRoomId, gameType, roomConfigId);
        if (roomOtherId == 0) {
            return false;
        }
        //退出房间
        int exited = roomManager.exitRoom(playerController);
        if (exited != Code.SUCCESS) {
            return false;
        }
        //加入房间
        int joined = roomManager.joinRoom(playerController, gameType, roomOtherId);
        return joined == Code.SUCCESS;
    }

    /**
     * 检查机器人添加逻辑
     */
    protected void checkAddRobot() {
        // 创建人数达到上限
        if (room.getRoomPlayers() != null && room.getRoomPlayers().size() >= room.getMaxLimit()) {
            return;
        }
        if (robotLastCreatedTime != 0 && robotLastCreatedTime > System.currentTimeMillis()) {
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
        robotLastCreatedTime = System.currentTimeMillis() + randomTime;
        int roomCfgId = roomCfg.getId();
        PlayerController robotPlayerController =
                roomManager.getRobotService().getOrCreateRobotPlayerController(roomCfgId, room.getId());
        if (robotPlayerController == null) {
            // 返回
            return;
        }
        // 将机器人加入房间中
        int code = roomManager.joinRoom(robotPlayerController, room.getGameType(), room.getId());
        // 如果加入失败则走一次退出房间逻辑
        if (code != Code.SUCCESS) {
            log.debug("机器人加入房间失败, code : {} {}", code, room.logStr());
        }
    }

    @Override
    public void hosting() {

    }

    @Override
    public void reconnect() {

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
            roomDao.saveRoom(room);
            this.room = room;
            // 退出房间时检查是否可以添加等待房间列表
            updateWaitRoomList();
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
            if (room != null) {
                /*log.debug("强制离开房间成功, gameType = {},roomId = {},playerIds = {}",
                    room.getRoomCfgId(),
                    room.getId(),
                    playerControllers.stream().map(PlayerController::playerId).map(String::valueOf).collect(Collectors.joining(",")));*/
            } else {
                log.debug("将玩家从房间中移除失败 gameType = {},roomId = {},playerIds = {}",
                        this.room.getRoomCfgId(),
                        this.room.getId(),
                        playerControllers.stream().map(PlayerController::playerId).map(String::valueOf).collect(Collectors.joining(",")));
                result.code = Code.FAIL;
                return result;
            }
            result.data = room;
            roomDao.saveRoom(room);
            this.room = room;
            // 退出房间时检查是否可以添加等待房间列表
            updateWaitRoomList();
        } catch (Exception e) {
            log.error("", e);
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
        // 如果同时需要发送某几个玩家有需要推送所有，则直接推送给所有人
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

    public void getRoomProcessor(BaseFuncProcessor roomProcessor) {
        this.roomProcessor = roomProcessor;
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

    public boolean isStoping() {
        return isStoping;
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
        log.info("开始暂停游戏 {}", room.logStr());
        this.isStoping = true;
        // 移除定时器
        this.timerCenter.remove(this);
        // 暂停游戏
        gameController.stopGame();
    }
}
