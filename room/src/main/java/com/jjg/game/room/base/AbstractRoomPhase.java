package com.jjg.game.room.base;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.timer.RoomPhaseTimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * 游戏阶段
 *
 * @author Administrator
 */
public abstract class AbstractRoomPhase<RC extends RoomCfg, G extends GameDataVo<RC>> implements IRoomPhase {
    protected static final Logger log = LoggerFactory.getLogger(AbstractRoomPhase.class);
    // 游戏维护的游戏数据
    protected G gameDataVo;
    // 游戏控制器
    protected AbstractGameController<RC, G> gameController;
    // 回合计数器
    protected int roundCounter;

    public AbstractRoomPhase(AbstractGameController<RC, G> gameController) {
        this.gameController = gameController;
        this.gameDataVo = gameController.getGameDataVo();
    }

    @Override
    public void phaseDoAction() {
        gameDataVo.setPhaseEndTime(System.currentTimeMillis() + getPhaseRunTime());
        gameDataVo.setPhaseRunTime(getPhaseRunTime());
    }

    /**
     * 玩家在阶段开始时的行为
     */
    @Override
    public void playerPhaseAction() {
        // 检查机器人行为
        for (Map.Entry<Long, GamePlayer> gamePlayerEntry : gameDataVo.getGamePlayerMap().entrySet()) {
            // 当前阶段的机器人应该执行的行为
            if (gamePlayerEntry.getValue() instanceof GameRobotPlayer robotPlayer) {
                robotAction(robotPlayer);
            } else {
                // 当前阶段的玩家托管行为
                if (gamePlayerEntry.getValue().isHosting()) {
                    hostingPlayerAction(gamePlayerEntry.getValue());
                }
            }
        }
    }

    /**
     * 托管玩家的行为
     */
    protected abstract void hostingPlayerAction(GamePlayer gamePlayer);

    /**
     * 机器人行为
     */
    protected abstract void robotAction(GameRobotPlayer gamePlayer);

    /**
     * 向房间广播消息
     */
    protected <M extends AbstractMessage> void broadcastBuilderToRoom(RoomMessageBuilder<M> message) {
        gameController.broadcastToPlayers(message);
    }

    /**
     * 向房间广播消息,全部玩家
     */
    protected <M extends AbstractMessage> void broadcastMsgToRoom(M message) {
        gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().toAllPlayer().setData(message));
    }

    /**
     * 添加阶段Timer,避免在当前阶段还在执行其他阶段的逻辑，如果定时器到了直接丢弃
     */
    protected void addPhaseTimer(TimerEvent<IProcessorHandler> event) {
        if (!(event.getTimerListener() instanceof AbstractGameController<?, ?>)) {
            log.error("添加阶段timer失败, 如果添加普通timeEvent可以自行实现，添加阶段TimeEvent必须在GameController中执行事件");
            return;
        }
        gameController.addGamePhaseTimer(
            new RoomPhaseTimeEvent<>(getGamePhase(), event, gameController.getRoom()));
    }

    @Override
    public boolean equals(Object o) {
        EGamePhase phaseName = getGamePhase();
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractRoomPhase<RC, G> that = (AbstractRoomPhase<RC, G>) o;
        return Objects.equals(phaseName, that.getGamePhase());
    }

    @Override
    public int hashCode() {
        EGamePhase phaseName = getGamePhase();
        return Objects.hashCode(phaseName);
    }

    public G getGameDataVo() {
        return gameDataVo;
    }

    @Override
    public void setRoundCounter(int roundCounter) {
        this.roundCounter = roundCounter;
    }

    @Override
    public int getRoundCounter() {
        return roundCounter;
    }
}
