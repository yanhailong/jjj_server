package com.jjg.game.room.base;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.timer.RoomPhaseTimeEvent;

import java.util.Map;
import java.util.Objects;

/**
 * 游戏阶段
 *
 * @author Administrator
 */
public abstract class AbstractRoomPhase<RC extends RoomCfg, G extends GameDataVo<RC>> implements IRoomPhase {
    // 游戏维护的游戏数据
    protected G gameDataVo;
    // 游戏控制器
    protected AbstractGameController<RC, G> gameController;

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
        gameController.sendMessage(message);
    }

    /**
     * 向房间广播消息
     */
    protected <M extends AbstractMessage> void broadcastMsgToRoom(M message) {
        gameController.sendMessage(RoomMessageBuilder.newBuilder().setData(message));
    }

    /**
     * 添加阶段Timer,避免在当前阶段还在执行其他阶段的逻辑，如果定时器到了直接丢弃
     */
    protected <E extends IProcessorHandler> void addPhaseTimer(TimerEvent<E> event) {
        gameController.addGamePhaseTimer(
            new RoomPhaseTimeEvent<>(getGamePhase(), event, gameController.getRoom()));
    }


    /**
     * 添加阶段Timer,避免在当前阶段还在执行其他阶段的逻辑，如果定时器到了直接丢弃
     */
    protected <E extends IProcessorHandler, R extends Room> void addPhaseTimer(RoomPhaseTimeEvent<E, R> event) {
        gameController.addGamePhaseTimer(event);
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
}
