package com.jjg.game.poker.game.blackjack.room;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.poker.game.blackjack.autohandler.BlackJackACEProcessorHandler;
import com.jjg.game.poker.game.blackjack.autohandler.BlackJackProcessorHandler;
import com.jjg.game.poker.game.blackjack.autohandler.BlackJackRobotHandler;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.blackjack.data.BlackJackBuilder;
import com.jjg.game.poker.game.blackjack.data.BlackJackDataHelper;
import com.jjg.game.poker.game.blackjack.gamephase.BlackJackBetPhase;
import com.jjg.game.poker.game.blackjack.gamephase.BlackJackPlayCardPhase;
import com.jjg.game.poker.game.blackjack.gamephase.BlackJackSettlementPhase;
import com.jjg.game.poker.game.blackjack.message.req.ReqBlackJackContinuedDeposit;
import com.jjg.game.poker.game.blackjack.message.resp.*;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.gamephase.BaseWaitReadyPhase;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerSampleCardOperation;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.common.message.req.ReqPokerSampleCardOperation;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.robot.RobotUtil;
import com.jjg.game.sampledata.bean.BlackjackCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.*;

import static com.jjg.game.poker.game.common.constant.PokerConstant.PlayerOperation.*;


/**
 * @author lm
 * @date 2025/7/28 14:03
 */
@GameController(gameType = EGameType.BLACK_JACK, roomType = RoomType.POKER_ROOM)
public class BlackJackGameController extends BasePokerGameController<BlackJackGameDataVo> {

    public BlackJackGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
        super(roomController);
    }

    @Override
    public void startNextRoundOrSettlement() {
        if (gameDataVo.getPlayerGameNnm() == 0) {
            setCurrentGamePhase(new BaseWaitReadyPhase<>(this));
            gameDataVo.resetData(this);
        } else {
            addPokerPhaseTimer(new BlackJackSettlementPhase(this));
        }
    }

    @Override
    public boolean canJoinRobot() {
        return getCurrentGamePhase() == EGamePhase.BET || getCurrentGamePhase() == EGamePhase.WAIT_READY;
    }

    @Override
    public PlayerSeatInfo getNextExePlayer() {
        int index = gameDataVo.getIndex();
        List<PlayerSeatInfo> seatInfos = gameDataVo.getPlayerSeatInfoList();
        if (index >= seatInfos.size() - 1) {
            return null;
        } else {
            Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
            int newIndex = seatInfos.size();
            for (int i = gameDataVo.getIndex() + 1; i < seatInfos.size(); i++) {
                PlayerSeatInfo info = seatInfos.get(i);
                if (info.isDelState()) {
                    continue;
                }
                int totalPoint = BlackJackDataHelper.getTotalPoint(info.getCurrentCards());
                if (totalPoint == BlackJackConstant.Common.PERFECT_POINT && info.getCards().size() == 1 && info.getCurrentCards().size() == roomCfg.getHandPoker()) {
                    continue;
                }
                newIndex = i;
                break;
            }
            if (newIndex > seatInfos.size() - 1) {
                return null;
            }
            gameDataVo.setIndex(newIndex);
        }
        return seatInfos.get(gameDataVo.getIndex());
    }

    @Override
    public void sampleCardOperation(long playerId, ReqPokerSampleCardOperation req) {
        switch (req.type) {
            case STOP -> dealStopCard(playerId, req.type);
            case GET_CARD -> dealPutCard(playerId, req);
            case CUT_CARD -> dealCutCard(playerId, req);
            case BUY_ACE -> dealBuyACE(playerId, req);
            case DOUBLE_BET -> dealDoubleBet(playerId, req);
        }
    }


    /**
     * 拿一张牌  停牌 下注
     */
    private void dealDoubleBet(long playerId, ReqPokerSampleCardOperation req) {
        NotifyBlackJackDoubleBetInfo notifyBlackJackDoubleBetInfo = new NotifyBlackJackDoubleBetInfo();
        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(seatInfo) || seatInfo.getPlayerId() != playerId) {
            notifyBlackJackDoubleBetInfo.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyBlackJackDoubleBetInfo));
            return;
        }
        //基础下注信息
        Long betValue = gameDataVo.getBaseBetInfo().get(playerId);
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (Objects.isNull(betValue) || Objects.isNull(gamePlayer)) {
            notifyBlackJackDoubleBetInfo.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyBlackJackDoubleBetInfo));
            return;
        }
        if (getTransactionItemNum(playerId) < betValue) {
            notifyBlackJackDoubleBetInfo.code = Code.NOT_ENOUGH;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyBlackJackDoubleBetInfo));
            return;
        }
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        if (seatInfo.getCurrentCards().size() > roomCfg.getHandPoker()) {
            notifyBlackJackDoubleBetInfo.code = Code.PARAM_ERROR;
            return;
        }
        //所有下注信息
        Map<Integer, Long> betInfo = gameDataVo.getAllBetInfo().computeIfAbsent(playerId, key -> new HashMap<>());
        betInfo.merge(seatInfo.getCardIndex(), betValue, Long::sum);
        deductItem(gamePlayer.getId(), betValue, AddType.GAME_BET);
        Thread.ofVirtual().start(() -> dealEffectiveBet(gamePlayer, betValue));
        int card = getCard(gameDataVo);
        seatInfo.getCurrentCards().add(card);
        seatInfo.setOperationType(req.type);
        notifyBlackJackDoubleBetInfo.betValue = betValue;
        notifyBlackJackDoubleBetInfo.betValueList = gameDataVo.getPlayerBetValueList().getOrDefault(playerId, new ArrayList<>());
        //还有牌组
        if (seatInfo.getCardIndex() + 1 < seatInfo.getCards().size()) {
            seatInfo.setCardIndex(seatInfo.getCardIndex() + 1);
            int autoCard = getCard(gameDataVo);
            seatInfo.getCurrentCards().add(autoCard);
            int totalPoint = BlackJackDataHelper.getTotalPoint(seatInfo.getCurrentCards());
            //自动发牌为21点
            if (totalPoint == BlackJackConstant.Common.PERFECT_POINT) {
                PlayerSeatInfo nextExePlayer = getNextExePlayer();
                if (Objects.isNull(nextExePlayer)) {
                    gameDataVo.setSettlementType(1);
                    gameDataVo.setSettlementDelayTime(2 * BlackJackDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS));
                    startNextRoundOrSettlement();
                    return;
                }
            }
            addNextTimer(seatInfo, 2);
            notifyBlackJackDoubleBetInfo.putCardInfo = BlackJackBuilder.getNotifyBlackJackPutCard(playerId, seatInfo, gameDataVo, card, autoCard);
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackDoubleBetInfo));
            return;
        }
        PlayerSeatInfo nextExePlayer = getNextExePlayer();
        if (Objects.nonNull(nextExePlayer)) {
            addNextTimer(nextExePlayer, 1);
            notifyBlackJackDoubleBetInfo.putCardInfo = BlackJackBuilder.getNotifyBlackJackPutCard(nextExePlayer.getPlayerId(), seatInfo, gameDataVo, card);
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackDoubleBetInfo));
        } else {
            gameDataVo.setSettlementType(2);
            gameDataVo.setSettlementDelayTime(BlackJackDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS));
            startNextRoundOrSettlement();
        }
    }

    /**
     * 21点 获取玩家总下注
     *
     * @param playerId 玩家id
     * @return 总下注
     */
    public long getPlayerTotalBet(long playerId) {
        Map<Long, Map<Integer, Long>> allBetInfo = gameDataVo.getAllBetInfo();
        Map<Integer, Long> betInfo = allBetInfo.get(playerId);
        if (Objects.isNull(betInfo)) {
            return 0;
        }
        long count = 0;
        for (Long betValue : betInfo.values()) {
            count += betValue;
        }
        return count;
    }

    /**
     * 21点 获取玩家单组牌总下注
     *
     * @param playerId 玩家id
     * @return 总下注
     */
    public long getPlayerSingleTotalBet(long playerId, int index) {
        Map<Long, Map<Integer, Long>> allBetInfo = gameDataVo.getAllBetInfo();
        Map<Integer, Long> betInfo = allBetInfo.get(playerId);
        if (Objects.isNull(betInfo)) {
            return 0;
        }
        return betInfo.getOrDefault(index, 0L);
    }

    /**
     * 处理购买ACE
     */
    private void dealBuyACE(long playerId, ReqPokerSampleCardOperation req) {
        NotifyBlackJackBetResult msg = new NotifyBlackJackBetResult();
        if (!gameDataVo.isCanBuyACE() || gameDataVo.getAceBuyPlayerIds().contains(playerId)) {
            msg.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, msg));
            return;
        }
        if (getCurrentGamePhase() != EGamePhase.PLAY_CART || gameDataVo.getAceBuyEndTime() == 0 || gameDataVo.getAceBuyEndTime() < System.currentTimeMillis()) {
            msg.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, msg));
            return;
        }
        List<PlayerSeatInfo> infoList = gameDataVo.getPlayerSeatInfoList();
        PlayerSeatInfo seatInfo = null;
        for (PlayerSeatInfo info : infoList) {
            if (info.getPlayerId() == playerId) {
                seatInfo = info;
                break;
            }
        }
        if (Objects.isNull(seatInfo)) {
            msg.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, msg));
            return;
        }
        Long betValue = gameDataVo.getBaseBetInfo().getOrDefault(playerId, 0L);
        if (betValue == 0) {
            msg.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, msg));
            return;
        }
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
        if (Objects.isNull(gamePlayer)) {
            msg.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, msg));
            return;
        }
        if (getTransactionItemNum(playerId) < betValue) {
            msg.code = Code.NOT_ENOUGH;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, msg));
            return;
        }
        deductItem(gamePlayer.getId(), betValue, AddType.GAME_BET);
        Thread.ofVirtual().start(() -> dealEffectiveBet(gamePlayer, betValue));
        gameDataVo.getAceBuyPlayerIds().add(playerId);
        //计算购买ace总金额
        long totalBet = 0;
        for (Long aceBuyPlayerId : gameDataVo.getAceBuyPlayerIds()) {
            totalBet += gameDataVo.getBaseBetInfo().getOrDefault(aceBuyPlayerId, 0L);
        }
        for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
            if (gameDataVo.getAceBuyPlayerIds().contains(info.getPlayerId())) {
                Long playerBetValue = gameDataVo.getBaseBetInfo().getOrDefault(info.getPlayerId(), 0L);
                NotifyBlackJackBetResult result = BlackJackBuilder.buildNotifyBlackJackBetResult(req.type, betValue, playerId, playerBetValue);
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), result));
            } else {
                NotifyBlackJackBetResult result = BlackJackBuilder.buildNotifyBlackJackBetResult(req.type, betValue, playerId, totalBet);
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), result));
            }
        }
        //全部下注完毕 通知ACE
        if (isAllOver(gameDataVo.getAceBuyPlayerIds())) {
            notifyAceResult();
        }

    }

    /**
     * 判断是否是全部完成操作
     *
     * @param overSet 玩家id
     * @return true 全部完成操作 false 存在未完成的玩家
     */
    public boolean isAllOver(Set<Long> overSet) {
        for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
            if (playerNotInit(seatInfo.getPlayerId())) {
                continue;
            }
            if (seatInfo.isSeatDown() && !overSet.contains(seatInfo.getPlayerId())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onPlayerLeaveRoomAction(RoomPlayer roomPlayer, SeatInfo remove) {
//        canStartNextPhase();
    }

    public void canStartNextPhase() {
        //下注阶段离开，并且离开时间在阶段结束时间大于500ms
        if (getCurrentGamePhase() == EGamePhase.BET && !gameDataVo.isPhaseEnd()) {
            Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
            if (CollectionUtil.isNotEmpty(baseBetInfo)) {
                //如果全部押注完成 进入下一阶段
                if (gameDataVo.canStartGame() && isAllOver(baseBetInfo.keySet())) {
                    genPlayerSeatInfoList(gameDataVo.getSeatInfo(), gameDataVo.getPlayerSeatInfoList());
                    removePokerPhaseTimer();
                    BlackJackPlayCardPhase gamePhase = new BlackJackPlayCardPhase(this);
                    addPokerPhase(gamePhase);
                }
            }
        }
    }

    @Override
    public void onRunGamePlayerLeaveRoom(SeatInfo remove) {
        PlayerSeatInfo currentPlayerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (currentPlayerSeatInfo.getPlayerId() == remove.getPlayerId()) {
            //如果他是执行人 直接下一轮或结算
            PlayerSeatInfo nextExePlayer = getNextExePlayer();
            if (Objects.isNull(nextExePlayer)) {
                //判断是否结算开启下一轮
                startNextRoundOrSettlement();
            } else {
                addNextPlayerAndBroadcast(nextExePlayer, new NotifyPokerSampleCardOperation());
            }
        }
    }

    /**
     * 通知ACE结果
     */
    public void notifyAceResult() {
        int totalPoint = BlackJackDataHelper.getTotalPoint(gameDataVo.getDealerCards());
        if (totalPoint == BlackJackConstant.Common.PERFECT_POINT) {
            startNextRoundOrSettlement();
            return;
        }
        NotifyBlackJackBuyACE msg = new NotifyBlackJackBuyACE();
        //设置第一个开始的玩家 并添加定时
        List<PlayerSeatInfo> playerSeatInfoList = gameDataVo.getPlayerSeatInfoList();
        for (int i = 0; i < playerSeatInfoList.size(); i++) {
            PlayerSeatInfo seatInfo = playerSeatInfoList.get(i);
            if (seatInfo.isDelState()) {
                continue;
            }
            gameDataVo.setIndex(i);
            break;
        }
        PlayerSeatInfo first = playerSeatInfoList.get(gameDataVo.getIndex());
        addNextTimer(first, 0);
        msg.nextPlayerId = first.getPlayerId();
        msg.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        msg.result = false;
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(msg));
    }


    @Override
    public boolean tryStartGame() {
        if (gameDataVo.canStartGame() && getCurrentGamePhase() == EGamePhase.WAIT_READY) {
            addPokerPhaseTimer(new BlackJackBetPhase(this, gameDataVo.getId()));
            return true;
        }
        return false;
    }

    @Override
    public void dealBet(long playerId, ReqPokerBet reqPokerBet) {
        NotifyBlackJackBetResult jackBetResult = new NotifyBlackJackBetResult();
        Pair<GamePlayer, List<Integer>> gamePlayerListPair = betActionAfterCheck(playerId);
        if (gamePlayerListPair == null) {
            jackBetResult.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
            return;
        }
        long betValue = reqPokerBet.betValue;
        List<Integer> betList = gamePlayerListPair.getSecond();
        GamePlayer gamePlayer = gamePlayerListPair.getFirst();
        if (!betList.contains(Long.valueOf(betValue).intValue())) {
            jackBetResult.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
            return;
        }
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        int check = betValueCheck(playerId, baseBetInfo, betValue);
        if (check > 0) {
            jackBetResult.code = check;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
            return;
        }
        deductItem(gamePlayer.getId(), betValue, AddType.GAME_BET);
        baseBetInfo.merge(playerId, betValue, Long::sum);
        Thread.ofVirtual().start(() -> dealEffectiveBet(gamePlayer, betValue));
        Map<Integer, Long> betInfo = gameDataVo.getAllBetInfo().computeIfAbsent(playerId, key -> new HashMap<>());
        betInfo.merge(0, betValue, Long::sum);
        gameDataVo.getPlayerBetValueList().computeIfAbsent(playerId, k -> new ArrayList<>()).add(betValue);
        jackBetResult.playerId = playerId;
        jackBetResult.type = reqPokerBet.betType;
        jackBetResult.betValue = betValue;
        jackBetResult.totalBetValue = baseBetInfo.get(playerId);
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(jackBetResult));
        //如果全部押注完成 进入下一阶段
//        canStartNextPhase();
    }

    public void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum) {
        addNextTimer(nextExePlayer, sendCardNum, 0);
    }

    public void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum, int times) {
        int time = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        int sendTime = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS);
        addPlayerTimer(new BlackJackProcessorHandler(nextExePlayer.getPlayerId(), gameDataVo.getId(), this),
                time + sendTime * sendCardNum + times);
        //机器人处理
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(nextExePlayer.getPlayerId());
        if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
            int chessExecutionDelay = RobotUtil.getChessExecutionDelay(robotPlayer.getActionId());
            BlackJackRobotHandler handler = new BlackJackRobotHandler(robotPlayer, BlackJackRobotHandler.DO_STRATEGY, this);
            RobotUtil.schedule(getRoomController(), handler, chessExecutionDelay);
        }
    }

    public void addACETimer(int sendCardNum) {
        int time = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.INSURANCE);
        int sendTime = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS);
        addPlayerTimer(new BlackJackACEProcessorHandler(gameDataVo.getId(), this),
                time + sendTime * sendCardNum);
    }

    /**
     * 处理分牌
     */
    private void dealCutCard(long playerId, ReqPokerSampleCardOperation req) {
        NotifyBlackJackCutCard notifyCutCard = new NotifyBlackJackCutCard();
        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(seatInfo) || seatInfo.getPlayerId() != playerId) {
            notifyCutCard.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyCutCard));
            return;
        }
        Long betValue = gameDataVo.getBaseBetInfo().getOrDefault(playerId, 0L);
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (betValue <= 0 || Objects.isNull(gamePlayer)) {
            notifyCutCard.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyCutCard));
            return;
        }
        if (getTransactionItemNum(playerId) < betValue) {
            notifyCutCard.code = Code.NOT_ENOUGH;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyCutCard));
        }
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        //只能分一次牌并且只能在发牌时分牌
        if (seatInfo.getCurrentCards().size() != roomCfg.getHandPoker() || seatInfo.getCardIndex() != 0 || seatInfo.getCards().size() != 1) {
            notifyCutCard.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyCutCard));
            return;
        }
        List<Integer> cards = seatInfo.getCurrentCards();
        Map<Integer, PokerCard> cardListMap = BlackJackDataHelper.getCardListMap(BlackJackDataHelper.getPoolId(gameDataVo));
        int firstCard = cardListMap.get(cards.get(0)).getRank();
        int secondCard = cardListMap.get(cards.get(1)).getRank();
        //判断是否能分牌
        if (firstCard != secondCard) {
            notifyCutCard.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyCutCard));
            return;
        }
        seatInfo.setOperationType(req.type);
        //进行分牌
        Integer first = cards.removeLast();
        List<List<Integer>> totalCards = seatInfo.getCards();
        List<Integer> secondList = new ArrayList<>();
        secondList.add(first);
        totalCards.add(secondList);
        int autoCard = getCard(gameDataVo);
        totalCards.getFirst().add(autoCard);
        //下注
        deductItem(gamePlayer.getId(), betValue, AddType.GAME_BET);
        Map<Integer, Long> betInfo = gameDataVo.getAllBetInfo().computeIfAbsent(playerId, key -> new HashMap<>());
        betInfo.merge(seatInfo.getCardIndex() + 1, betValue, Long::sum);
        Thread.ofVirtual().start(() -> dealEffectiveBet(gamePlayer, betValue));
        int totalPoint = BlackJackDataHelper.getTotalPoint(seatInfo.getCurrentCards());
        int sendCardNum = 1;
        notifyCutCard.playerId = playerId;
        notifyCutCard.cardInfoList = BlackJackBuilder.getCardInfos(seatInfo, this);
        //判断是否是21点
        if (totalPoint == BlackJackConstant.Common.PERFECT_POINT) {
            //判断
            seatInfo.setCardIndex(seatInfo.getCardIndex() + 1);
            List<Integer> newCards = seatInfo.getCurrentCards();
            autoCard = getCard(gameDataVo);
            newCards.add(autoCard);
            notifyCutCard.cardInfoList = BlackJackBuilder.getCardInfos(seatInfo, this);
            notifyCutCard.autoCard = BlackJackDataHelper.getClientCardId(gameDataVo, autoCard);
            if (BlackJackDataHelper.getTotalPoint(seatInfo.getCurrentCards()) == BlackJackConstant.Common.PERFECT_POINT) {
                PlayerSeatInfo nextExePlayer = getNextExePlayer();
                if (Objects.isNull(nextExePlayer)) {
                    //直接进行结算
                    gameDataVo.setSettlementType(1);
                    gameDataVo.setSettlementDelayTime(2 * BlackJackDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS) + BlackJackConstant.Common.CUT_FIX_TIME);
                    gameDataVo.setShowDealer(isAllCutCard());
                    startNextRoundOrSettlement();
                    return;
                }
                seatInfo = nextExePlayer;
                sendCardNum = 2;
            }
        }
        //修正500毫秒
        addNextTimer(seatInfo, sendCardNum, BlackJackConstant.Common.CUT_FIX_TIME);
        notifyCutCard.operationId = seatInfo.getPlayerId();
        notifyCutCard.betValueList = gameDataVo.getPlayerBetValueList().getOrDefault(playerId, new ArrayList<>());
        //通知分牌结果
        notifyCutCard.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        notifyCutCard.currentCardIds = seatInfo.getCardIndex();
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyCutCard));
    }


    /**
     *
     */
    public boolean isAllCutCard() {
        List<PlayerSeatInfo> playerSeatInfoList = gameDataVo.getPlayerSeatInfoList();
        for (PlayerSeatInfo seatInfo : playerSeatInfoList) {
            if (seatInfo.isDelState()) {
                continue;
            }
            if (seatInfo.getOperationType() != CUT_CARD) {
                return false;
            }
        }
        return true;
    }

    /**
     * 处理拿牌
     */
    private void dealPutCard(long playerId, ReqPokerSampleCardOperation req) {
        NotifyBlackJackPutCard notified = new NotifyBlackJackPutCard();
        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(seatInfo) || playerId != seatInfo.getPlayerId()) {
            notified.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notified));
            return;
        }
        List<Integer> cards = seatInfo.getCurrentCards();
        int card = getCard(gameDataVo);
        cards.add(card);
        //判断是否到21点和爆
        int sum = BlackJackDataHelper.getTotalPoint(cards);
        notified.playerId = seatInfo.getPlayerId();
        notified.cardId = BlackJackDataHelper.getClientCardId(gameDataVo, card);
        notified.totalPoint = BlackJackDataHelper.getShowTotalPoint(cards);
        seatInfo.setOperationType(req.type);
        PlayerSeatInfo nextExePlayer = seatInfo;
        //21点和爆
        if (sum >= BlackJackConstant.Common.PERFECT_POINT || cards.size() >= BlackJackConstant.Common.MAX_GET_CARD) {
            //第二副牌
            if (seatInfo.getCardIndex() + 1 < seatInfo.getCards().size()) {
                seatInfo.setCardIndex(seatInfo.getCardIndex() + 1);
                int autoCard = getCard(gameDataVo);
                seatInfo.getCurrentCards().add(autoCard);
                notified.autoCardId = BlackJackDataHelper.getClientCardId(gameDataVo, autoCard);
                //判断是否是21点
                int totalPoint = BlackJackDataHelper.getTotalPoint(seatInfo.getCurrentCards());
                notified.nextTotalPoint = BlackJackDataHelper.getShowTotalPoint(seatInfo.getCurrentCards());
                notified.cardIndex = nextExePlayer.getCardIndex();
                if (totalPoint == BlackJackConstant.Common.PERFECT_POINT) {
                    nextExePlayer = getNextExePlayer();
                    if (Objects.isNull(nextExePlayer)) {
                        gameDataVo.setSettlementType(1);
                        gameDataVo.setSettlementDelayTime(2 * BlackJackDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS));
                        startNextRoundOrSettlement();
                        return;
                    }
                }
                addNextTimer(nextExePlayer, 2);
                notified.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
                //分牌后前端索引需要加1
                notified.operationId = nextExePlayer.getPlayerId();
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notified));
                return;
            }
            seatInfo.setOver(true);
            notified.cardIndex = seatInfo.getCards().size() > 1 ? seatInfo.getCardIndex() + 1 : seatInfo.getCardIndex();
            //获取下一个玩家
            nextExePlayer = getNextExePlayer();
            if (Objects.isNull(nextExePlayer)) {
                notified.playerId = seatInfo.getPlayerId();
                gameDataVo.setSettlementType(2);
                gameDataVo.setSettlementDelayTime(BlackJackDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS));
                startNextRoundOrSettlement();
                return;
            }
        }
        addNextTimer(nextExePlayer, 1);
        notified.playerId = seatInfo.getPlayerId();
        notified.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        //分牌后前端索引需要加1
        notified.cardIndex = seatInfo.getCards().size() > 1 ? seatInfo.getCardIndex() + 1 : seatInfo.getCardIndex();
        notified.operationId = nextExePlayer.getPlayerId();
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notified));
    }

    /**
     * 获取下一张牌
     */
    public int getCard(BlackJackGameDataVo gameDataVo) {
        List<Integer> cards = gameDataVo.getCards();
        if (cards.isEmpty()) {
            if (Objects.nonNull(gameDataVo.getTempCard())) {
                gameDataVo.setCards(new ArrayList<>(gameDataVo.getTempCard()));
                cards = gameDataVo.getCards();
            } else {
                Map<Integer, PokerCard> cardListMap = BlackJackDataHelper.getCardListMap(BlackJackDataHelper.getPoolId(gameDataVo));
                gameDataVo.setCards(new ArrayList<>(cardListMap.keySet()));
                cards = gameDataVo.getCards();
                Collections.shuffle(cards);
                gameDataVo.setCards(cards);
            }
        }
        return cards.removeFirst();
    }


    /**
     * 处理停牌
     */
    public void dealStopCard(long playerId, int type) {
        NotifyBlackJackStopCard msg = new NotifyBlackJackStopCard();
        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(seatInfo) || seatInfo.getPlayerId() != playerId) {
            msg.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, msg));
            return;
        }
        NotifyPokerSampleCardOperation notify = new NotifyPokerSampleCardOperation();
        msg.operation = notify;
        //设置操作类型
        seatInfo.setOperationType(type);
        notify.playerId = seatInfo.getPlayerId();
        notify.operationType = type;
        //玩家分牌停牌
        if (seatInfo.getCardIndex() + 1 < seatInfo.getCards().size()) {
            //玩家分牌停牌
            seatInfo.setCardIndex(seatInfo.getCardIndex() + 1);
            int autoCard = getCard(gameDataVo);
            seatInfo.getCurrentCards().add(autoCard);
            msg.autoCardId = BlackJackDataHelper.getClientCardId(gameDataVo, autoCard);
            msg.autoCardTotal = BlackJackDataHelper.getShowTotalPoint(seatInfo.getCurrentCards());
            int totalPoint = BlackJackDataHelper.getTotalPoint(seatInfo.getCurrentCards());
            if (totalPoint == BlackJackConstant.Common.PERFECT_POINT) {
                PlayerSeatInfo nextExePlayer = getNextExePlayer();
                if (Objects.isNull(nextExePlayer)) {
                    gameDataVo.setSettlementType(1);
                    gameDataVo.setSettlementDelayTime(BlackJackDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS));
                    startNextRoundOrSettlement();
                    return;
                }
                addNextTimer(nextExePlayer, 1);
                notify.nextPlayerId = nextExePlayer.getPlayerId();
                notify.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(msg));
                return;
            }
            addNextTimer(seatInfo, 1);
            //通知
            notify.nextPlayerId = seatInfo.getPlayerId();
            notify.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(msg));
        } else {
            //本轮操作完成
            seatInfo.setOver(true);
            //通知
            //获取下一个玩家
            PlayerSeatInfo nextExePlayer = getNextExePlayer();
            if (Objects.nonNull(nextExePlayer)) {
                addNextTimer(nextExePlayer, 0);
                notify.nextPlayerId = nextExePlayer.getPlayerId();
                notify.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(msg));
            } else {
                startNextRoundOrSettlement();
            }
        }
    }

    @Override
    protected BlackJackGameDataVo createRoomDataVo(Room_ChessCfg roomCfg) {
        return new BlackJackGameDataVo(roomCfg);

    }


    @Override
    public void respRoomInitInfoAction(PlayerController playerController) {
        RepsBlackJackRoomBaseInfo baseInfo = new RepsBlackJackRoomBaseInfo();
        baseInfo.settlementInfo = gameDataVo.getSettlementInfo();
        Map<Long, PlayerSeatInfo> playerSeatInfoMap = gameDataVo.getPlayerSeatInfoMap();
        baseInfo.playerInfos = new ArrayList<>();
        baseInfo.phase = getCurrentGamePhase();
        for (Map.Entry<Integer, SeatInfo> entry : gameDataVo.getSeatInfo().entrySet()) {
            SeatInfo seatInfo = entry.getValue();
            if (playerNotInit(seatInfo.getPlayerId()) || !seatInfo.isSeatDown()) {
                continue;
            }
            baseInfo.playerInfos.add(BlackJackBuilder.getBlackJackPlayerInfo(playerSeatInfoMap.get(seatInfo.getPlayerId()), seatInfo, this));
        }
        if (baseInfo.phase == EGamePhase.PLAY_CART) {
            baseInfo.cardIds = new ArrayList<>();
            baseInfo.cardIds.add(BlackJackDataHelper.getClientCardId(gameDataVo, gameDataVo.getDealerCards().getFirst()));
            if (gameDataVo.getAceBuyEndTime() == 0 || gameDataVo.getAceBuyEndTime() < System.currentTimeMillis()) {
                if (Objects.nonNull(gameDataVo.getPlayerTimerEvent())) {
                    baseInfo.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
                    baseInfo.operationId = gameDataVo.getCurrentPlayerSeatInfo().getPlayerId();
                }
            }
            long aceTotalBet = 0;
            long aceBet = 0;
            for (Long aceBuyPlayerId : gameDataVo.getAceBuyPlayerIds()) {
                Long playerAceBet = gameDataVo.getBaseBetInfo().getOrDefault(aceBuyPlayerId, 0L);
                aceTotalBet += playerAceBet;
                if (aceBuyPlayerId == playerController.playerId()) {
                    aceBet = playerAceBet;
                }
            }
            baseInfo.aceTotalBet = aceTotalBet;
            baseInfo.aceBet = aceBet;
        }
        if (baseInfo.phase == EGamePhase.BET) {
            baseInfo.overTime = gameDataVo.getPhaseEndTime();
        }
        BlackjackCfg blackjackCfg = BlackJackDataHelper.getBlackjackCfg(gameDataVo);
        baseInfo.chipsList = blackjackCfg.getBetList();
        baseInfo.betTime = BlackJackDataHelper.getExecutionTime(gameDataVo, PokerPhase.BET);
        baseInfo.operationTime = BlackJackDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        baseInfo.sendCardTime = BlackJackDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS);
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerController.playerId(), baseInfo));
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.BLACK_JACK;
    }

    /**
     * 请求续押
     *
     * @param playerId 玩家id
     * @param req 请求
     */
    public void reqBlackJackContinuedDeposit(long playerId, ReqBlackJackContinuedDeposit req) {
        NotifyBlackJackContinuedDeposit jackBetResult = new NotifyBlackJackContinuedDeposit();
        Pair<GamePlayer, List<Integer>> gamePlayerListPair = betActionAfterCheck(playerId);
        if (gamePlayerListPair == null) {
            jackBetResult.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
            return;
        }
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        if (baseBetInfo.containsKey(playerId)) {
            jackBetResult.code = Code.FORBID;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
            return;
        }
        long totalBet = 0;
        GamePlayer gamePlayer = gamePlayerListPair.getFirst();
        List<Long> betValueList = req.betValueList;
        List<Integer> betList = gamePlayerListPair.getSecond();
        for (Long betValue : betValueList) {
            if (!betList.contains(betValue.intValue())) {
                jackBetResult.code = Code.PARAM_ERROR;
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
                return;
            }
            totalBet += betValue;
        }
        int check = betValueCheck(playerId, baseBetInfo, totalBet);
        if (check > 0) {
            jackBetResult.code = check;
            return;
        }
        deductItem(gamePlayer.getId(), totalBet, AddType.GAME_BET);
        baseBetInfo.merge(playerId, totalBet, Long::sum);
        long finalTotalBet = totalBet;
        Thread.ofVirtual().start(() -> dealEffectiveBet(gamePlayer, finalTotalBet));
        gameDataVo.getPlayerBetValueList().computeIfAbsent(playerId, k -> new ArrayList<>()).addAll(betValueList);
        Map<Integer, Long> betInfo = gameDataVo.getAllBetInfo().computeIfAbsent(playerId, key -> new HashMap<>());
        betInfo.merge(0, totalBet, Long::sum);
        jackBetResult.playerId = playerId;
        jackBetResult.betValueList = betValueList;
        jackBetResult.totalBetValue = baseBetInfo.get(playerId);
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(jackBetResult));
    }

    /**
     * 下注金额检查
     * @param playerId 玩家id
     * @param baseBetInfo 下注信息
     * @param totalBet 总下注
     * @return 错误码
     */
    private int betValueCheck(long playerId, Map<Long, Long> baseBetInfo, long totalBet) {
        BlackjackCfg blackjackCfg = BlackJackDataHelper.getBlackjackCfg(gameDataVo);
        Long alreadyBet = baseBetInfo.getOrDefault(playerId, 0L);
        if (totalBet + alreadyBet > blackjackCfg.getLimit()) {
            return Code.BET_TO_LIMIT;
        }
        //金币判断
        if (getTransactionItemNum(playerId) < totalBet) {
            return Code.NOT_ENOUGH;
        }
        return 0;
    }

    /**
     * 下注前检查
     * @param playerId 玩家id
     */
    private Pair<GamePlayer, List<Integer>> betActionAfterCheck(long playerId) {
        if (getCurrentGamePhase() != EGamePhase.BET) {
            return null;
        }
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (Objects.isNull(gamePlayer)) {
            return null;
        }
        //加入时间小于修正时间禁止下注
        if (gamePlayer.getPokerPlayerGameData().getJoinTime() + BlackJackConstant.Common.BET_FIX_TIME > gameDataVo.getPhaseEndTime()) {
            return null;
        }
        //进行押注
        SeatInfo mySeatInfo = null;
        for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
            if (seatInfo.getPlayerId() == playerId) {
                mySeatInfo = seatInfo;
                break;
            }
        }
        if (Objects.isNull(mySeatInfo)) {
            return null;

        }
        BlackjackCfg blackjackCfg = BlackJackDataHelper.getBlackjackCfg(gameDataVo);
        //下注列表
        List<Integer> betList = blackjackCfg.getBetList();
        if (betList == null || betList.isEmpty()) {
            return null;
        }
        return Pair.newPair(gamePlayer, betList);
    }
}
