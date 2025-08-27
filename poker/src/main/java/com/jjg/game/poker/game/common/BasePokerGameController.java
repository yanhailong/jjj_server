package com.jjg.game.poker.game.common;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BaseWaitReadyPhase;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerPlayerChange;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerSampleCardOperation;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.common.message.req.ReqPokerSampleCardOperation;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.room.base.EGameState;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.PokerPlayerGameData;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.timer.RoomEventType;
import com.jjg.game.room.timer.RoomTimerEvent;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.*;
import java.util.stream.Collectors;

import static com.jjg.game.room.timer.RoomEventType.POKER_PLAYER_EVENT;
import static com.jjg.game.room.timer.RoomEventType.ROOM_PHASE_RUN_EVENT;

/**
 * @author lm
 * @date 2025/7/26 10:06
 */
public abstract class BasePokerGameController<T extends BasePokerGameDataVo> extends AbstractPhaseGameController<Room_ChessCfg, T> {

    private TimerEvent<IProcessorHandler> currentGameTimerEvent;

    public BasePokerGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
        super(roomController);
        currentGamePhase = new BaseWaitReadyPhase<>(this);
    }

    public boolean inRunPhase() {
        return getCurrentGamePhase() == EGamePhase.PLAY_CART;
    }

    /**
     * 重载通知全部时 只发送在座位中的玩家
     */
    @Override
    public <M extends AbstractMessage> void broadcastToPlayers(RoomMessageBuilder<M> message) {
        if (message.isToAll()) {
            Set<Long> playerIds = gameDataVo.getSeatInfo().values()
                .stream()
                .map(SeatInfo::getPlayerId)
                .filter(playerId -> {
                    GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
                    return Objects.nonNull(gamePlayer) && gamePlayer.getPokerPlayerGameData().isInit();
                })
                .collect(Collectors.toSet());
            message.setPlayerIds(playerIds);
            message.setToAll(false);
            roomController.broadcastToPlayers(message);
            return;
        }
        roomController.broadcastToPlayers(message);

    }

    public void genPlayerSeatInfoList(Map<Integer, SeatInfo> seatInfoMap, List<PlayerSeatInfo> playerSeatInfoList) {
        for (Map.Entry<Integer, SeatInfo> entry : seatInfoMap.entrySet()) {
            SeatInfo info = entry.getValue();
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(info.getPlayerId());
            if (Objects.isNull(gamePlayer)) {
                log.error("扑克确定执行顺序时GamePlayer 为null playerId:{} id:{}", info.getPlayerId(), gameDataVo.getId());
                continue;
            }
            if (gamePlayer.getPokerPlayerGameData().isInit() && info.isSeatDown()) {
                info.setJoinGame(true);
                playerSeatInfoList.add(new PlayerSeatInfo(entry.getKey(), info.getPlayerId()));
            } else {
                info.setJoinGame(false);
            }
        }
    }

    /**
     * 开启下一轮执行 还是直接结算
     */
    public abstract void startNextRoundOrSettlement();

    /**
     * 获取下一个执行人
     */
    public abstract PlayerSeatInfo getNextExePlayer();

    /**
     * 设置游戏当前阶段
     */
    public void setCurrentGamePhase(IRoomPhase currentGamePhase) {
        this.currentGamePhase = currentGamePhase;
    }

    /**
     * 简单牌操作
     */
    public abstract void sampleCardOperation(long playerId, ReqPokerSampleCardOperation req);

    /**
     * 添加节点执行timer
     *
     * @param phase 阶段定时器
     */
    public final void addPokerPhaseTimer(IRoomPhase phase) {
        currentGamePhase = phase;
        phase.phaseDoAction();
        phase.playerPhaseAction();
        IProcessorHandler handler = phase::phaseFinish;
        currentGameTimerEvent = new TimerEvent<>(this, phase.getPhaseRunTime(), handler);
        addGameTimeEvent(currentGameTimerEvent, ROOM_PHASE_RUN_EVENT);
    }

    /**
     * 添加阶段执行
     *
     * @param phase 阶段
     */
    public final void addPokerPhase(IRoomPhase phase) {
        currentGamePhase = phase;
        phase.phaseDoAction();
        phase.playerPhaseAction();
    }


    @Override
    public void addGameTimeEvent(TimerEvent<IProcessorHandler> roomUpdateTimer, RoomEventType roomEventType) {
        RoomTimerEvent<IProcessorHandler, Room> timerEvent = new RoomTimerEvent<>(roomUpdateTimer,
            roomController.getRoom(), roomEventType);
        timerCenter.add(timerEvent);
        if (POKER_PLAYER_EVENT == roomEventType) {
            gameDataVo.setPlayerTimerEvent(timerEvent);
        }
    }

    /**
     * 添加玩家定时器
     */
    public final void addPlayerTimer(IProcessorHandler handler, int time) {
        long exeTime = System.currentTimeMillis() + time;
        TimerEvent<IProcessorHandler> playerGameTimerEvent = new TimerEvent<>(this, exeTime, handler);
        if (Objects.nonNull(gameDataVo.getPlayerTimerEvent())) {
            removePlayerTimerEvent(gameDataVo.getPlayerTimerEvent());
        }
        addGameTimeEvent(playerGameTimerEvent, POKER_PLAYER_EVENT);
    }

    /**
     * 移除玩家定时器
     */
    public void removePlayerTimerEvent(RoomTimerEvent<IProcessorHandler, Room> event) {
        if (Objects.nonNull(event)) {
            timerCenter.remove(this, event.getParameter());
        }
    }

    /**
     * 移除阶段定时器
     */
    public final void removePokerPhaseTimer() {
        if (Objects.nonNull(currentGameTimerEvent)) {
            timerCenter.remove(this, currentGameTimerEvent.getParameter());
        }
    }

    /**
     * 跳转到等待阶段
     */
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
        NotifyPokerPlayerChange playerChange = new NotifyPokerPlayerChange();
        playerChange.pokerPlayerInfo =
            PokerBuilder.buildPlayerInfo(gameDataVo.getGamePlayer(playerController.playerId()), null, gameDataVo);
        playerChange.totalNum = gameDataVo.getGamePlayerMap().size();
        roomController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(playerChange).exceptPlayer(playerController.playerId()));
        //尝试开启游戏
        tryStartNextGame();
    }

    /**
     * 玩家请求初始化房间信息行为
     */
    public abstract void respRoomInitInfoAction(PlayerController playerController);

    @Override
    public void continueGame() {
        tryStartNextGame();
        gameState = EGameState.GAMING;
    }

    public boolean playerNotInit(long playerId) {
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        return (Objects.isNull(gamePlayer) || Objects.isNull(gamePlayer.getPokerPlayerGameData()) || !gamePlayer.getPokerPlayerGameData().isInit());
    }

    /**
     * 尝试开启下一轮游戏
     */
    public void tryStartNextGame() {
        if (!checkRoomCanNextRound()) {
            broadcastGamePauseInfo();
            gameState = EGameState.PAUSED;
            return;
        }
        tryStartGame();
    }

    /**
     * 尝试开启游戏
     */
    public abstract void tryStartGame();


    public int isSeatDown(Map<Integer, SeatInfo> seatInfoList, long playerId) {
        for (Map.Entry<Integer, SeatInfo> entry : seatInfoList.entrySet()) {
            SeatInfo seatInfo = entry.getValue();
            if (seatInfo.getPlayerId() == playerId) {
                return entry.getKey();
            }
        }
        return -1;
    }

    @Override
    protected final GamePlayer onPlayerJoinRoom(PlayerController playerController, boolean gameStartStatus) {
        GamePlayer gamePlayer = super.onPlayerJoinRoom(playerController, gameStartStatus);
        RoomPlayer roomPlayer = getRoom().getRoomPlayers().get(gamePlayer.getId());
        PokerPlayerGameData pokerPlayerGameData = new PokerPlayerGameData();
        pokerPlayerGameData.setJoinTime(System.currentTimeMillis());
        gamePlayer.setPokerPlayerGameData(pokerPlayerGameData);
        try {
            onPlayerJoinRoomAction(gamePlayer);
        } catch (Exception e) {
            log.error("onPlayerJoinRoomAction() failed", e);
        }
        //存放座位信息 如果座位有人了随便找一个 找不到
        Map<Integer, SeatInfo> seatInfoList = gameDataVo.getSeatInfo();
        int seatDown = isSeatDown(seatInfoList, playerController.playerId());
        if (seatDown != -1) {
            roomPlayer.setSit(seatDown);
            return gamePlayer;
        }
        SeatInfo seatInfo = seatInfoList.get(roomPlayer.getSit());
        if (Objects.isNull(seatInfo)) {
            seatInfoList.put(roomPlayer.getSit(), getSeatInfo(gamePlayer, roomPlayer));
        } else {
            for (int i = 0; i < getRoom().getMaxLimit(); i++) {
                if (!seatInfoList.containsKey(i)) {
                    roomPlayer.setSit(i);
                    seatInfoList.put(roomPlayer.getSit(), getSeatInfo(gamePlayer, roomPlayer));
                    break;
                }
            }
        }
        return gamePlayer;
    }

    private SeatInfo getSeatInfo(GamePlayer gamePlayer, RoomPlayer roomPlayer) {
        SeatInfo seatInfo = new SeatInfo();
        seatInfo.setPlayerId(gamePlayer.getId());
        seatInfo.setJoinGame(false);
        seatInfo.setSeatDown(true);
        seatInfo.setSeatId(roomPlayer.getSit());
        return seatInfo;
    }

    public void onPlayerJoinRoomAction(GamePlayer gamePlayer) {

    }

    public void onPlayerLeaveRoomAction(RoomPlayer roomPlayer, SeatInfo remove) {

    }

    /**
     * 运行时玩家座位改变
     */
    public void runPlayerSeatChange(SeatInfo remove, boolean isPlaying) {
        //正在游玩
        List<PlayerSeatInfo> playerSeatInfos = gameDataVo.getPlayerSeatInfoList();
        int index = -1;
        if (isPlaying) {
            for (int i = 0; i < playerSeatInfos.size(); i++) {
                PlayerSeatInfo seatInfo = playerSeatInfos.get(i);
                if (seatInfo.getPlayerId() == remove.getPlayerId()) {
                    index = i;
                    seatInfo.setDelState(true);
                    seatInfo.setOver(true);
                    break;
                }
            }
            remove.setJoinGame(false);
        }
        if (index != -1) {
            onRunGamePlayerLeaveRoom(remove);
        }

    }


    public abstract void onRunGamePlayerLeaveRoom(SeatInfo remove);

    public void addNextPlayerAndBroadcast(PlayerSeatInfo nextExePlayer,
                                          NotifyPokerSampleCardOperation notifyPokerSampleCardOperation) {
        addNextTimer(nextExePlayer, 0);
        notifyPokerSampleCardOperation.nextPlayerId = nextExePlayer.getPlayerId();
        notifyPokerSampleCardOperation.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPokerSampleCardOperation));
    }

    /**
     * 添加下一个玩家定时器
     *
     * @param nextExePlayer
     * @param sendCardNum
     */
    public abstract void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum);

    @Override
    public <R extends Room> CommonResult<R> onPlayerLeaveRoom(PlayerController playerController) {
        // 通知场上玩家离开
        RoomPlayer roomPlayer = getRoom().getRoomPlayers().get(playerController.playerId());
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerController.playerId());
        if (Objects.nonNull(gamePlayer) && Objects.nonNull(roomPlayer)) {
            //移除座位信息
            SeatInfo remove = gameDataVo.getSeatInfo().remove(roomPlayer.getSit());
            if (Objects.nonNull(remove)) {
                //移除下注信息
                gameDataVo.getBaseBetInfo().remove(roomPlayer.getPlayerId());
                if (inRunPhase()) {
                    runPlayerSeatChange(remove, remove.isSeatDown() && remove.isJoinGame());
                }
                remove.setSeatDown(false);
                try {
                    onPlayerLeaveRoomAction(roomPlayer, remove);
                } catch (Exception e) {
                    log.error("onPlayerLeaveRoomAction() failed", e);
                }
            }
            NotifyPokerPlayerChange playerChange = new NotifyPokerPlayerChange();
            playerChange.pokerPlayerInfo = PokerBuilder.buildPlayerInfo(gamePlayer, remove, gameDataVo);
            roomController.broadcastToPlayers(RoomMessageBuilder.newBuilder()
                .toAllPlayer().exceptPlayer(playerController.playerId())
                .setData(playerChange));
        }
        return super.onPlayerLeaveRoom(playerController);
    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return true;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.add(new BaseWaitReadyPhase<>(this));
        return linkedHashSet;
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public void autoRunGamePhase() {
    }

    /**
     * 处理下注
     */
    public abstract void dealBet(long playerId, ReqPokerBet reqPokerBet);
}
