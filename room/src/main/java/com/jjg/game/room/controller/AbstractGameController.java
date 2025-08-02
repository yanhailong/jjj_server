package com.jjg.game.room.controller;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.*;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.core.utils.ReflectionTool;
import com.jjg.game.room.base.IPhaseMsgAdapter;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.timer.RoomEventType;
import com.jjg.game.room.timer.RoomPhaseTimeEvent;
import com.jjg.game.room.timer.RoomTimerCenter;
import com.jjg.game.room.timer.RoomTimerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 抽象游戏流程控制器
 *
 * @author 2CL
 */
public abstract class AbstractGameController<RC extends RoomCfg, G extends GameDataVo<RC>> implements TimerListener<IProcessorHandler>,
        IGameController, IGameLifeCycle {
    protected static final Logger log = LoggerFactory.getLogger(AbstractGameController.class);
    // 游戏配置
    protected G gameDataVo;
    // 游戏控制器
    protected AbstractRoomController<RC, ? extends Room> roomController;
    // 游戏定时器，用于更新游戏中的操作逻辑，不要直接将引用暴露到外面，有需要的逻辑需要在此类中添加
    protected RoomTimerCenter timerCenter;
    // 游戏的阶段处理，通过有序的列表，遍历游戏
    private LinkedHashSet<IRoomPhase> gamePhases = new LinkedHashSet<>();
    // 当前的游戏阶段
    protected IRoomPhase currentGamePhase;
    // 阶段运行计数器
    protected AtomicInteger roundCounter = new AtomicInteger(0);
    // 游戏阶段的迭代器,在每个游戏结束时进行重置
    private Iterator<IRoomPhase> gamePhaseIterator;
    // 游戏是否开始
    private boolean gameStarted = false;

    public AbstractGameController(AbstractRoomController<RC, ? extends Room> roomController) {
        this.roomController = roomController;
    }

    /**
     * 开始游戏
     */
    @Override
    public void startGame() {
        // 记录开始时间
        gameDataVo.setStartTime(System.currentTimeMillis());
        // 初始化游戏阶段配置
        LinkedHashSet<IRoomPhase> initedGamePhaseConf = initGamePhaseConf();
        log.info("{} 启动加载: {} 个阶段逻辑", this.getClass().getSimpleName(), initedGamePhaseConf.size());
        gamePhases = initedGamePhaseConf;
        roundCounter.set(0);
        // 初始化迭代器
        gamePhaseIterator = gamePhases.iterator();
        // 标记房间开始运行
        gameStarted = true;
        // 开始
        autoRunGamePhase();
    }

    /**
     * 房间逻辑开始运转
     */
    @Override
    public void autoRunGamePhase() {
        // 房间是否还有逻辑需要执行
        if (gamePhaseIterator.hasNext() && gameStarted) {
            // 当前的游戏阶段
            currentGamePhase = gamePhaseIterator.next();
            currentGamePhase.setRoundCounter(roundCounter.get());
            // 执行当前阶段的逻辑
            try {
                currentGamePhase.phaseDoAction();
                // 调用玩家的行为,主要是机器人和托管的玩家
                currentGamePhase.playerPhaseAction();
            } catch (Exception ex) {
                // 发生异常中断，不能阻断流程，先打出日志
                log.error("运行阶段：{} 开始时发生异常！msg: {}",
                        currentGamePhase.getGamePhase().getPhaseName(), ex.getMessage(), ex);
            }
            // 将阶段逻辑添加到
            addGameTimeEvent(new TimerEvent<>(this, currentGamePhase.getPhaseRunTime(),
                    () -> {
                        try {
                            // 定时器时间到,调用结束逻辑
                            currentGamePhase.phaseFinish();
                        } catch (Exception ex) {
                            // 发生异常中断，不能阻断流程，先打出日志
                            log.error("运行阶段：{} 结束时发生异常！msg: {}",
                                    currentGamePhase.getGamePhase().getPhaseName(), ex.getMessage(), ex);
                        }
                        // 如果有绑定的下一个阶段可以切换到
                        IRoomPhase bindNextPhase = currentGamePhase.bindNextPhase();
                        if (bindNextPhase != null) {
                            Iterator<IRoomPhase> latestGamePhase = gamePhaseIterator;
                            while (gamePhaseIterator.hasNext()) {
                                // 需要将迭代器跳到绑定的阶段,然后继续执行逻辑
                                latestGamePhase = gamePhaseIterator;
                                if (gamePhaseIterator.next() == bindNextPhase) {
                                    break;
                                }
                            }
                            gamePhaseIterator = latestGamePhase;
                        }
                        // 自动切换到下一个阶段
                        this.autoRunGamePhase();
                    }), RoomEventType.ROOM_PHASE_RUN_EVENT);
        } else {
            // 阶段全部运行结束
            phaseRunOver();
            // 全部游戏阶段完成
            roomPhaseRoundOver();
        }
    }

    /**
     * 一个回合的房间逻辑结束
     */
    private void roomPhaseRoundOver() {
        // 判断房间是否全部结束
        if (isGameOverAfterPhaseOver()) {
            // 调用roomController的游戏结束逻辑
            roomController.gameOver();
        } else {
            // 进入下一轮之前调用
            beforeEnterNextRound();
            // 初始化迭代器
            gamePhaseIterator = gamePhases.iterator();
            // 回合计数++
            roundCounter.incrementAndGet();
            // 自动进入下一轮
            autoRunGamePhase();
        }
    }

    /**
     * 是否能退出游戏
     *
     * @param playerId 玩家id
     * @return true 能 false 不能
     */
    public boolean canExitGame(long playerId) {
        return true;
    }

    /**
     * 进入下一轮游戏之前调用
     */
    protected void beforeEnterNextRound() {
    }

    /**
     * 检查房间开局逻辑,默认房间进入玩家并且房间未开始，则开启房间逻辑，实际的房间开启逻辑需要自行判断
     */
    protected boolean checkRoomCanStart() {
        // 房间玩家不为空
        return !roomController.getRoom().getRoomPlayers().isEmpty() && !gameStarted;
    }

    /**
     * 玩家加入房间时调用
     *
     * @return 返回进行数据复制后的GamePlayer对象
     */
    protected GamePlayer onPlayerJoinRoom(PlayerController playerController, boolean gameStartStatus) {
        // 将玩家数据复制到玩家游戏数据中
        Player player = playerController.getPlayer();
        String playerJson = JSON.toJSONString(player);
        GamePlayer gamePlayer;
        if (player instanceof RobotPlayer) {
            gamePlayer = JSON.parseObject(playerJson, GameRobotPlayer.class);
            gameDataVo.addGamePlayer(gamePlayer);
        } else {
            gamePlayer = JSON.parseObject(playerJson, GamePlayer.class);
            gameDataVo.addGamePlayer(gamePlayer);
        }
        // 当玩家中途加入阶段时
        if (gameStarted) {
            currentGamePhase.onPlayerHalfwayJoinPhase(gamePlayer);
        }
        return gamePlayer;
    }

    /**
     * 玩家发送房间初始信息 客户端在刚进入房间时，不能收到服务端的主动推送，所以需要等客户端初始化完成后，主动向服务端请求
     */
    public abstract void respRoomInitInfo(PlayerController playerController);

    /**
     * 在单个游戏阶段结束后，判断房间是否全部结束
     *
     * @return 是否结束整个游戏
     */
    protected abstract boolean isGameOverAfterPhaseOver();

    /**
     * 初始化游戏阶段配置，游戏中的整体逻辑均由每个小的阶段构成一体
     */
    protected abstract LinkedHashSet<IRoomPhase> initGamePhaseConf();

    /**
     * 通过初始的RoomDataVo基类，子类自行实例化符合当前的类的VO对象
     */
    protected abstract G createRoomDataVo(RC roomCfg);

    /**
     * 游戏的单轮的所有的阶段都完成了调用
     */
    protected abstract void phaseRunOver();

    /**
     * 处理每个游戏逻辑消息
     *
     * @param message 消息
     * @param <M>     消息泛型
     */
    public <M extends AbstractMessage> void dispatchGamePhaseMsg(PlayerController playerController, PFMessage message) {
        List<IPhaseMsgAdapter<M>> phaseMsgAdapters =
                gamePhases.stream().filter(gamePhase -> gamePhase instanceof IPhaseMsgAdapter<?>)
                        .map(gamePhase -> (IPhaseMsgAdapter<M>) gamePhase).toList();
        int msgId = message.cmd;
        if (phaseMsgAdapters.isEmpty()) {
            log.error("异常请求，当前房间：{}  cfgId: {} 需要主动接受请求的阶段为空，但是客户端还是在主动请求",
                    gameDataVo.getRoomId(), gameDataVo.getRoomCfg().getId());
            return;
        }
        for (IPhaseMsgAdapter<M> phaseMsgAdapter : phaseMsgAdapters) {
            if (phaseMsgAdapter.reqMsgId() == msgId) {
                // 如果请求的消息不在此阶段，则直接报错
                if (currentGamePhase.getGamePhase() != phaseMsgAdapter.getGamePhase()) {
                    log.error("玩家ID: {} 房间不处于阶段：{} 却还在请求,房间当前阶段: {}",
                            playerController.playerId(), phaseMsgAdapter.getGamePhase(), currentGamePhase.getGamePhase());
                } else {
                    Set<Class<AbstractMessage>> actualTypes =
                            ReflectionTool.getClassSuperActualType(phaseMsgAdapter.getClass(), AbstractMessage.class);
                    if (actualTypes.isEmpty()) {
                        return;
                    }
                    Class<AbstractMessage> mClass = null;
                    for (Class<AbstractMessage> actualType : actualTypes) {
                        if (AbstractMessage.class.isAssignableFrom(actualType)) {
                            mClass = actualType;
                            break;
                        }
                    }
                    if (mClass == null) {
                        EGameType eGameType = EGameType.getGameByTypeId(gameDataVo.getRoomCfg().getGameID());
                        // 消息没有继承AbstractMessage类
                        log.error("游戏类型：{} 游戏阶段：{} 消息ID: {} 没有实现AbstractMessage类",
                                eGameType.getGameDesc(),
                                currentGamePhase.getGamePhase().getPhaseName(),
                                Integer.toHexString(msgId).toUpperCase());
                        return;
                    }
                    M reqMessage = (M) ProtostuffUtil.deserialize(message.data, mClass);
                    log.debug("处理房间：{} 消息：{}", gameDataVo.getRoomId(), JSON.toJSON(reqMessage));
                    // 具体的处理逻辑方法
                    phaseMsgAdapter.dealMsg(playerController, reqMessage);
                }
            }
        }
    }

    /**
     * 发送消息 消息
     */
    public <M extends AbstractMessage> void broadcastToPlayers(RoomMessageBuilder<M> message) {
        roomController.broadcastToPlayers(message);
    }


    /**
     * 初始化计时器
     */
    public void initTimerCenter(RoomTimerCenter timerCenter) {
        this.timerCenter = timerCenter;
    }

    // 更新游戏
    @Override
    public void onTimer(TimerEvent<IProcessorHandler> event) {
        if (event == null || event.getParameter() == null) {
            return;
        }
        if (event instanceof RoomPhaseTimeEvent<?, ?> roomPhaseTimeEvent) {
            // 当前房间阶段
            EGamePhase curGamePhase = currentGamePhase.getGamePhase();
            // 添加事件时处于的阶段
            EGamePhase eventGamePhase = roomPhaseTimeEvent.geteGamePhase();
            // 如果定时任务的游戏阶段不能跨阶段执行，且当前阶段不为添加事件时的阶段则丢弃此任务
            if (!roomPhaseTimeEvent.isCanAcrossPhaseExec() && !curGamePhase.equals(eventGamePhase)) {
                return;
            }
        }
        try {
            // 执行事件的回调
            event.getParameter().action();
        } catch (Exception ex) {
            log.error("房间内的定时器更新逻辑异常, {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public CommonResult<Room> onPlayerLeaveRoom(PlayerController playerController) {
        GamePlayer gamePlayer = gameDataVo.getGamePlayerMap().get(playerController.playerId());
        // 玩家中途离开阶段时调用
        currentGamePhase.onPlayerHalfwayExitPhase(gamePlayer);
        // 从玩家列表中移除玩家数据，子类的gameDataVo有和玩家相关的临时数据需要自行删除
        gameDataVo.getGamePlayerMap().remove(playerController.playerId());
        return new CommonResult<>(Code.SUCCESS);
    }

    @Override
    public void reconnect() {

    }

    @Override
    public void hosting() {
        // 玩家挂机之后，将玩家状态切位挂机状态
    }

    @Override
    public void timeTick() {
        // 每100ms调用一次 定时回存房间数据
    }

    @Override
    public void roomReady() {

    }

    @Override
    public void disbandRoom() {
        // 先暂停房间类的阶段执行逻辑
        gameStarted = false;
        // 再调用游戏内的解散房间逻辑
        gamePhases.stream()
                .filter(iGamePhase -> iGamePhase.getGamePhase().equals(EGamePhase.DISS_MISS))
                .forEach(IRoomPhase::phaseDoAction);
    }

    /**
     * 由房间控制器调用此方法
     */
    @Override
    public void gameOver() {
        // 暂停游戏
        stopGame();
        // 调用结算逻辑
        gameOverSettlement();
        // 调用房间管理器的解散逻辑
        roomController.getRoomManager().disbandRoom(roomController.getRoom());
    }

    @Override
    public void gameOverSettlement() {
        // 整局结束进入大结算
    }

    public G getGameDataVo() {
        return gameDataVo;
    }

    public void setGameDataVo(G gameDataVo) {
        this.gameDataVo = gameDataVo;
    }


    /**
     * 给游戏添加定时器
     */
    public void addGameTimeEvent(TimerEvent<IProcessorHandler> roomUpdateTimer, RoomEventType roomEventType) {
        timerCenter.add(new RoomTimerEvent<>(roomUpdateTimer, roomController.getRoom(), roomEventType));
    }

    /**
     * 设置游戏当前阶段
     */
    public void setCurrentGamePhase(IRoomPhase currentGamePhase) {
        this.currentGamePhase = currentGamePhase;
    }

    /**
     * 获取游戏当前处于哪个阶段
     */
    public EGamePhase getCurrentGamePhase() {
        return currentGamePhase.getGamePhase();
    }

    /**
     * 获取房间统计信息
     */
    public void getRoomStatistics() {

    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    @Override
    public void stopGame() {
        gameStarted = false;
        gameDataVo.setStopTime(System.currentTimeMillis());
        // 房间结束前调用
        beforeDestroyRoom();
        // 暂停计时器运行
        this.timerCenter.remove(this);
    }

    public <E extends RoomTimerEvent<IProcessorHandler, Room>> void addGamePhaseTimer(E roomTimerEvent) {
        timerCenter.add(roomTimerEvent);
    }

    public AbstractRoomController<RC, ? extends Room> getRoomController() {
        return roomController;
    }

    public <R extends Room> R getRoom() {
        return (R) roomController.getRoom();
    }
}
