package com.jjg.game.poker.game.common;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BaseWaitReadyPhase;
import com.jjg.game.poker.game.common.message.reps.NotifyPlayerChange;
import com.jjg.game.poker.game.common.message.req.ReqSampleCardOperation;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.PokerPlayerGameData;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

import static com.jjg.game.room.timer.RoomEventType.PLAYER_EVENT;
import static com.jjg.game.room.timer.RoomEventType.ROOM_PHASE_RUN_EVENT;

/**
 * @author lm
 * @date 2025/7/26 10:06
 */
public abstract class BasePokerGameController<T extends BasePokerGameDataVo> extends AbstractGameController<Room_ChessCfg, T> {

    private TimerEvent<IProcessorHandler> currentGameTimerEvent;

    public BasePokerGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
        super(roomController);
        currentGamePhase = new BaseWaitReadyPhase<>(this);
    }

    /**
     * 简单牌操作
     */
    public abstract void sampleCardOperation(long playerId, ReqSampleCardOperation req);

    public final void addPokerPhaseTimer(IRoomPhase phase) {
        currentGamePhase = phase;
        phase.phaseDoAction();
        phase.playerPhaseAction();
        IProcessorHandler handler = phase::phaseFinish;
        currentGameTimerEvent = new TimerEvent<>(this, phase.getPhaseRunTime(), handler);
        addGameTimeEvent(currentGameTimerEvent, ROOM_PHASE_RUN_EVENT);
    }

    public final void addPokerPhase(IRoomPhase phase) {
        currentGamePhase = phase;
        phase.phaseDoAction();
        phase.playerPhaseAction();
    }

    public final void addPlayerTimer(PlayerSeatInfo playerSeatInfo, IProcessorHandler handler, int time) {
        TimerEvent<IProcessorHandler> playerGameTimerEvent = new TimerEvent<>(this, time, handler);
        if (Objects.nonNull(playerSeatInfo.getPlayerGameTimerEvent())) {
            timerCenter.remove(this, currentGameTimerEvent);
        }
        playerSeatInfo.setPlayerGameTimerEvent(playerGameTimerEvent);
        addGameTimeEvent(currentGameTimerEvent, PLAYER_EVENT);
    }

    public final void removePokerPhaseTimer() {
        if (Objects.nonNull(currentGameTimerEvent)) {
            timerCenter.remove(this, currentGameTimerEvent);
        }
    }


    public final void goBackWaitReadyPhase() {
        removePokerPhaseTimer();
        setCurrentGamePhase(new BaseWaitReadyPhase<>(this));
    }

    @Override
    public final void respRoomInitInfo(PlayerController playerController) {
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerController.playerId());
        gamePlayer.getPokerPlayerGameData().setInit(true);
        respRoomInitInfoAction(playerController);
        //通知其他玩家 玩家加入
        NotifyPlayerChange playerChange = new NotifyPlayerChange();
        playerChange.playerInfo = PokerBuilder.buildPlayerInfo(gameDataVo.getGamePlayer(playerController.playerId()), gameDataVo);
        playerChange.totalNum = gameDataVo.getGamePlayerMap().size();
        roomController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(playerChange));
        //尝试开启游戏
        tryStartGame();
    }

    public abstract void respRoomInitInfoAction(PlayerController playerController);

    /**
     * 达到游戏开始的条件执行
     */
    public abstract void tryStartGame();


    @Override
    protected final GamePlayer onPlayerJoinRoom(PlayerController playerController, boolean gameStartStatus) {
        GamePlayer gamePlayer = super.onPlayerJoinRoom(playerController, gameStartStatus);
        RoomPlayer roomPlayer = getRoom().getRoomPlayers().get(gamePlayer.getId());
        PokerPlayerGameData pokerPlayerGameData = new PokerPlayerGameData();
        pokerPlayerGameData.setJoinTime(System.currentTimeMillis());
        gamePlayer.setPokerPlayerGameData(pokerPlayerGameData);
        //存放座位信息 如果座位有人了随便找一个 找不到
        Map<Integer, SeatInfo> seatInfoList = gameDataVo.getSeatInfo();
        if (!seatInfoList.containsKey(roomPlayer.getSit())) {
            SeatInfo seatInfo = new SeatInfo();
            seatInfo.setPlayerId(gamePlayer.getId());
            seatInfo.setJoinGame(false);
            seatInfo.setSeatDown(true);
            seatInfo.setSeatId(roomPlayer.getSit());
            seatInfoList.put(roomPlayer.getSit(), seatInfo);
        } else {
            for (int i = 0; i < getRoom().getMaxLimit(); i++) {
                if (!seatInfoList.containsKey(i)) {
                    roomPlayer.setSit(i);
                    SeatInfo seatInfo = new SeatInfo();
                    seatInfo.setPlayerId(gamePlayer.getId());
                    seatInfo.setJoinGame(false);
                    seatInfo.setSeatDown(true);
                    seatInfo.setSeatId(roomPlayer.getSit());
                    seatInfoList.put(roomPlayer.getSit(), seatInfo);
                    break;
                }
            }
        }
        //TODO 找不到能坐的位置怎么办
        try {
            onPlayerJoinRoomAction(gamePlayer);
        } catch (Exception e) {
            log.error("onPlayerJoinRoomAction() failed", e);
        }
        return gamePlayer;
    }

    public void onPlayerJoinRoomAction(GamePlayer gamePlayer) {

    }

    public void onPlayerLeaveRoomAction(PlayerController playerController, SeatInfo remove) {

    }

    @Override
    public CommonResult<Room> onPlayerLeaveRoom(PlayerController playerController) {
        // 通知场上玩家离开
        RoomPlayer roomPlayer = getRoom().getRoomPlayers().get(playerController.playerId());
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerController.playerId());
        if (Objects.nonNull(gamePlayer) && Objects.nonNull(roomPlayer)) {
            NotifyPlayerChange playerChange = new NotifyPlayerChange();
            playerChange.playerInfo = PokerBuilder.buildPlayerInfo(gamePlayer, gameDataVo);
            roomController.broadcastToPlayers(RoomMessageBuilder.newBuilder()
                    .toAllPlayer().exceptPlayer(playerController.playerId())
                    .setData(playerChange));
            SeatInfo remove = gameDataVo.getSeatInfo().remove(roomPlayer.getSit());
            try {
                onPlayerLeaveRoomAction(playerController,remove);
            } catch (Exception e) {
                log.error("onPlayerLeaveRoomAction() failed", e);
            }
        }
        return super.onPlayerLeaveRoom(playerController);
    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return true;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        return null;
    }


    @Override
    protected void phaseRunOver() {

    }


}
