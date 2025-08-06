package com.jjg.game.room.controller;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.core.utils.ReflectionTool;
import com.jjg.game.room.base.IPhaseMsgAdapter;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.timer.RoomEventType;
import com.jjg.game.room.timer.RoomPhaseTimeEvent;
import com.jjg.game.room.timer.RoomTimerEvent;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通过阶段运转的游戏控制器
 *
 * @author 2CL
 */
public abstract class AbstractPhaseGameController<RC extends RoomCfg, G extends GameDataVo<RC>> extends AbstractGameController<RC, G> {

    // 游戏的阶段处理，通过有序的列表，遍历游戏
    private LinkedHashSet<IRoomPhase> gamePhases = new LinkedHashSet<>();
    // 当前的游戏阶段
    protected IRoomPhase currentGamePhase;
    // 阶段运行计数器
    protected AtomicInteger roundCounter = new AtomicInteger(0);
    // 游戏阶段的迭代器,在每个游戏结束时进行重置
    private Iterator<IRoomPhase> gamePhaseIterator;

    public AbstractPhaseGameController(AbstractRoomController<RC, ? extends Room> roomController) {
        super(roomController);
    }

    @Override
    public void startGame() {
        super.startGame();
        // 初始化游戏阶段配置
        LinkedHashSet<IRoomPhase> initedGamePhaseConf = initGamePhaseConf();
        log.info("{} 启动加载: {} 个阶段逻辑", this.getClass().getSimpleName(), initedGamePhaseConf.size());
        gamePhases = initedGamePhaseConf;
        roundCounter.set(0);
        // 初始化迭代器
        gamePhaseIterator = gamePhases.iterator();
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
                    // 如果有绑定的下一个阶段可以切换，具体的绑定逻辑需要自行判断是否需要跳阶段
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
     * 游戏的单轮的所有的阶段都完成了调用
     */
    protected abstract void phaseRunOver();

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
     * 添加阶段timeEvent，event的触发与阶段绑定，timeEvent的只允许在设置的阶段时间内运行
     */
    public <E extends RoomTimerEvent<IProcessorHandler, Room>> void addGamePhaseTimer(E roomTimerEvent) {
        timerCenter.add(roomTimerEvent);
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

    @Override
    protected GamePlayer onPlayerJoinRoom(PlayerController playerController, boolean gameStartStatus) {
        GamePlayer gamePlayer = super.onPlayerJoinRoom(playerController, gameStartStatus);
        // 当玩家中途加入阶段时
        if (gameStarted) {
            currentGamePhase.onPlayerHalfwayJoinPhase(gamePlayer);
        }
        return gamePlayer;
    }

    @Override
    public <R extends Room> CommonResult<R> onPlayerLeaveRoom(PlayerController playerController) {
        GamePlayer gamePlayer = gameDataVo.getGamePlayerMap().get(playerController.playerId());
        // 玩家中途离开阶段时调用
        currentGamePhase.onPlayerHalfwayExitPhase(gamePlayer);
        // 从玩家列表中移除玩家数据，子类的gameDataVo有和玩家相关的临时数据需要自行删除
        gameDataVo.getGamePlayerMap().remove(playerController.playerId());
        // 玩家退出时直接回存玩家数据，需要放在游戏离开逻辑最后
        directlySavePlayerData(gamePlayer);
        return new CommonResult<>(Code.SUCCESS);
    }

    @Override
    public void disbandRoom() {
        super.disbandRoom();
        // 再调用游戏内的解散房间逻辑
        gamePhases.stream()
            .filter(iGamePhase -> iGamePhase.getGamePhase().equals(EGamePhase.DISS_MISS))
            .forEach(IRoomPhase::phaseDoAction);
    }

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
                // 如果请求在此阶段，直接处理
                if (currentGamePhase.getGamePhase() == phaseMsgAdapter.getGamePhase()) {
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
                        // 消息没有继承AbstractMessage类
                        log.error("{} 游戏阶段：{} 消息ID: {} 对应的消息类没有继承AbstractMessage类",
                            gameDataVo.roomLogInfo(),
                            currentGamePhase.getGamePhase().getPhaseName(),
                            Integer.toHexString(msgId).toUpperCase());
                        return;
                    }
                    M reqMessage = (M) ProtostuffUtil.deserialize(message.data, mClass);
                    log.debug("处理房间：{} 消息：{}", gameDataVo.getRoomId(), JSON.toJSON(reqMessage));
                    // 具体的处理逻辑方法
                    phaseMsgAdapter.dealMsg(playerController, reqMessage);
                    break;
                }
            }
        }
    }

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
        if (currentGamePhase == null) {
            log.error("获取游戏的阶段为空，此情况不应该出现，{}", gameDataVo.roomLogInfo());
            return null;
        }
        return currentGamePhase.getGamePhase();
    }

    /**
     * 通过阶段类型获取房间阶段，如果需要
     */
    public IRoomPhase findRoomPhase(EGamePhase eGamePhase) {
        for (IRoomPhase gamePhase : gamePhases) {
            if (gamePhase.getGamePhase() == eGamePhase) {
                return gamePhase;
            }
        }
        return null;
    }
}
