package com.jjg.game.room.controller;

import com.jjg.game.common.concurrent.BaseFuncProcessor;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.dao.PlayerRoomDataDao;
import com.jjg.game.core.data.*;
import com.jjg.game.room.manager.AbstractRoomManager;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayFlowPojo;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.timer.RoomTimerCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    public AbstractRoomController(Class<? extends RoomPlayer> roomPlayerClazz, R room) {
        this.roomPlayerClazz = roomPlayerClazz;
        this.room = room;
    }

    /**
     * 通过配置创建游戏controller
     *
     * @param gameDataVo 配置
     */
    public <RD extends GameDataVo<RC>> AbstractGameController<RC, RD> createGameController(GameDataVo<RC> gameDataVo) {
        Set<Class<? extends AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>>> gameControllerClazz =
                roomManager.getGameControllerClazz();
        try {
            for (Class<? extends AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> controllerClazz :
                    gameControllerClazz) {
                GameController gameAnnotateController = controllerClazz.getAnnotation(GameController.class);
                EGameType games = gameAnnotateController.gameType();
                if (games.getGameTypeId() == gameDataVo.getRoomCfg().getGameID()) {
                    Constructor<AbstractGameController<RC, RD>> constructor =
                            (Constructor<AbstractGameController<RC, RD>>) controllerClazz.getDeclaredConstructor(AbstractRoomController.class);
                    // 将调用当前方法的RoomController写入GameController中
                    AbstractGameController<RC, RD> gameController = constructor.newInstance(this);
                    RD roomDataVoCopied = gameController.copyRoomDataVo(gameDataVo);
                    roomDataVoCopied.setRoomId(room.getId());
                    gameController.setGameDataVo(roomDataVoCopied);
                    gameController.initTimerCenter(timerCenter);
                    return gameController;
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
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
                    log.warn("加入房间失败 gameType = {},roomId = {},playerId = {}",
                            playerController.getPlayer().getGameType(),
                            this.room.getId(),
                            playerController.playerId());
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
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 向指定房间的指定玩家广播消息。
     *
     * @param <T>      the type of the message
     * @param playerId 房间中玩家的ID
     * @param message  要发送的消息，可以是任何类型
     */
    public <T> void sendToPlayer(long playerId, T message) {
        if (message == null) {
            log.warn("消息为空，发送房间广播消息失败");
            return;
        }
        PlayerController playerController = playerControllers.get(playerId);
        if (Objects.isNull(playerController)) {
            log.warn("玩家不在线 玩家id:{}", playerId);
            return;
        }
        playerController.send(message);
    }

    /**
     * 更新房间等待列表
     */
    private void updateWaitRoomList() {
        // 如果房间已满，则需要将等待房间列表从redis中删除
        if (!room.canEnter()) {
            roomManager.getMatchDataDao().removeWaitJoinRoomId(room.getGameType(), room.getRoomCfgId(), room.getId());
        } else {
            // 如果房间未满，则将直接写入等待列表
            roomManager.getMatchDataDao().addWaitJoinRoomId(room.getGameType(), room.getRoomCfgId(), room.getId(),
                    room.getCreateTime());
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
                    public Boolean updateDataWithRes(Room r) {
                        try {
                            //先检查该玩家是否已经在该房间中
                            boolean exist = r.hasPlayer(playerController.playerId());
                            if (exist) {
                                log.debug("玩家已在房间中 gameType = {},roomId = {},playerId = {}",
                                        playerController.getPlayer().getGameType(), thatRoom.getId(),
                                        playerController.playerId());
                                return true;
                            }

                            if (!r.canEnter()) {
                                log.debug("该房间无法进入 gameType = {},roomId = {},playerId = {}",
                                        playerController.getPlayer().getGameType(), thatRoom.getId(),
                                        playerController.playerId());
                                return false;
                            }

                            //如果之前不在房间，则按座位排座
                            for (int i = 0; i < r.getMaxLimit(); i++) {
                                boolean flag = r.setHasPlayer(i);
                                if (!flag) {
                                    RoomPlayer tmpRoomPlayer =
                                            roomDao.createRoomPlayer(playerController.playerId());
                                    tmpRoomPlayer.setSit(i);
                                    tmpRoomPlayer.setPlayer(playerController.getPlayer());
                                    r.addPlayer(tmpRoomPlayer);
                                    return true;
                                }
                            }
                        } catch (Exception e) {
                            log.error("", e);
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
        gameController.initGame();
        // 调用子类的启动方法
        gameController.startGame();
    }

    @Override
    public void onTimer(TimerEvent<IProcessorHandler> e) {

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
        // 时间管理移除当前注册器
        timerCenter.remove(this);
        // 调用房间控制器中的解散房间逻辑
        gameController.disbandRoom();
        // 回存房间数据
        saveRoomData();
        // 向玩家发送解散房间消息
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
            playerRoomDataList.add(playerRoomData);
        }
        // 回存玩家房间数据
        playerRoomDataDao.saveAll(playerRoomDataList);
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
    public void initGame() {
        // 当前房间的线程实例，用于投递一些异步任务
        roomProcessor = roomManager.getProcessorExecutors().getProcessorById(room.getId());
        // 创建游戏控制器
        gameController = createGameController(new GameDataVo<>(roomCfg));
    }

    @Override
    public void roomReady() {

    }

    @Override
    public void roomTick() {
        // 游戏启动后进行tick
        if (gameController.isGameStarted()) {
            gameController.roomTick();
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
    public CommonResult<Room> onPlayerLeaveRoom(PlayerController playerController) {
        CommonResult<Room> result = new CommonResult<>(Code.SUCCESS);
        try {
            boolean remove = roomDao.removePlayer(
                    playerController.getPlayer().getGameType(),
                    playerController.roomId(),
                    playerController.playerId());
            if (remove) {
                log.debug("强制离开房间成功, gameType = {},roomId = {},playerId = {}",
                        playerController.getPlayer().getGameType(), playerController.roomId(),
                        playerController.playerId());
            }
            // 调用游戏的离开房间逻辑
            gameController.onPlayerLeaveRoom(playerController);
            // 移除玩家PlayerController
            playerControllers.remove(playerController.getPlayer().getId());
            //从room中移除
            RoomPlayer roomPlayer = room.exit(playerController.playerId());
            if (roomPlayer == null) {
                log.debug("将玩家从房间中移除失败 gameType = {},roomId = {},playerId = {}", room.getGameType(), room.getId(),
                        playerController.playerId());
                result.code = Code.FAIL;
                return result;
            }
            log.debug("退出房间成功，但是还没有广播玩家退出 roomId = {},playerId = {}", room.getId(), playerController.playerId());
            result.data = room;
            roomDao.saveRoom(room);
            // 退出房间时检查是否可以添加等待房间列表
            updateWaitRoomList();
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 向房间里的所有玩家发送消息
     *
     * @param <T> the type of the message
     * @param msg the message to be sent to all players in the room
     */
    public <T> void broadcastToRoomAllPlayers(T msg) {
        broadcastToPlayers(playerControllers.keySet(), msg);
    }

    /**
     * 向指定房间的玩家广播消息。
     *
     * @param <T>         the type of the message
     * @param roomPlayers 房间中玩家的ID集合
     * @param message     要发送的消息，可以是任何类型
     */
    public <T> void broadcastToPlayers(Iterable<Long> roomPlayers, T message) {
        if (message == null) {
            log.warn("消息为空，发送房间广播消息失败");
            return;
        }
        for (Long roomPlayerId : roomPlayers) {
            PlayerController playerController = playerControllers.get(roomPlayerId);
            if (Objects.nonNull(playerController) && !(playerController.getPlayer() instanceof RobotPlayer)) {
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

    public void setPlayerControllers(Map<Long, PlayerController> playerControllers) {
        this.playerControllers = playerControllers;
    }

    public void addPlayerController(PlayerController playerController) {
        playerControllers.put(playerController.playerId(), playerController);
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
}
