package com.jjg.game.poker.game.blackjack.room;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.blackjack.data.BlackJackDataHelper;
import com.jjg.game.poker.game.blackjack.gamephase.BlackJackProcessorHandler;
import com.jjg.game.poker.game.blackjack.gamephase.BlackJackSettlementPhase;
import com.jjg.game.poker.game.blackjack.gamephase.BlackJackStartGamePhase;
import com.jjg.game.poker.game.blackjack.message.bean.BlackJackCardInfo;
import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackBetResult;
import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackCutCard;
import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackPutCard;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerSampleCardOperation;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.common.message.req.ReqPokerSampleCardOperation;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.*;

import static com.jjg.game.poker.game.common.constant.PokerConstant.PlayerOperation.*;


/**
 * @author lm
 * @date 2025/7/28 14:03
 */
public class BlackJackGameController extends BasePokerGameController<BlackJackGameDataVo> {

    public BlackJackGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
        super(roomController);
    }

    @Override
    public void startNextRoundOrSettlement() {
        addPokerPhaseTimer(new BlackJackSettlementPhase(this));
    }

    @Override
    public PlayerSeatInfo getNextExePlayer() {
        int index = gameDataVo.getIndex();
        List<PlayerSeatInfo> seatInfos = gameDataVo.getPlayerSeatInfoList();
        if (index >= seatInfos.size() - 1) {
            return null;
        } else {
            Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
            int newIndex = seatInfos.size() - 1;
            for (int i = gameDataVo.getIndex() + 1; i < seatInfos.size(); i++) {
                PlayerSeatInfo info = seatInfos.get(i);
                int totalPoint = BlackJackDataHelper.getTotalPoint(info.getCurrentCards());
                if (totalPoint == BlackJackConstant.Common.PERFECT_POINT && info.getCards().size() == 1 && info.getCurrentCards().size() == roomCfg.getHandPoker()) {
                    continue;
                }
                newIndex = i;
                break;
            }
            if (index >= seatInfos.size() - 1) {
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
        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(seatInfo) || seatInfo.getPlayerId() != playerId) {
            return;
        }
        //下注

    }


    /**
     * 处理购买ACE
     *
     * @param playerId
     * @param req
     */
    private void dealBuyACE(long playerId, ReqPokerSampleCardOperation req) {
        NotifyPokerSampleCardOperation operation = new NotifyPokerSampleCardOperation();
        if (!gameDataVo.isCanBuyACE() || gameDataVo.getAceBuyPlayerIds().contains(playerId)) {
            return;
        }
        if (getCurrentGamePhase() != EGamePhase.PLAY_CART) {
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
            return;
        }
        Long betValue = gameDataVo.getBaseBet().getOrDefault(playerId, 0L);
        if (betValue == 0) {
            return;
        }
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
        if (Objects.isNull(gamePlayer)) {
            return;
        }
        if (gamePlayer.getGold() < betValue) {
            operation.code = Code.NOT_ENOUGH;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, operation));
            return;
        }
        gamePlayer.setGold(gamePlayer.getGold() - betValue);
        gameDataVo.getAceBuyPlayerIds().add(playerId);
        //购买ACE
        operation.operationType = req.type;
        operation.playerId = playerId;
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(operation));
    }


    @Override
    public void tryStartGame() {
        addPokerPhaseTimer(new BlackJackStartGamePhase(this));
    }

    @Override
    public void dealBet(long playerId, ReqPokerBet reqPokerBet) {
        if (getCurrentGamePhase() != EGamePhase.BET) {
            return;
        }
        NotifyBlackJackBetResult jackBetResult = new NotifyBlackJackBetResult();
        //进行押注
        PlayerSeatInfo playerSeatInfo = null;
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.getPlayerId() == playerId) {
                playerSeatInfo = info;
                break;
            }
        }
        if (playerSeatInfo == null) {
            jackBetResult.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
            return;
        }
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        List<Integer> betList = roomCfg.getBetList();
        long betValue = jackBetResult.betValue;
        if (betList == null || betList.isEmpty() || !betList.contains(Long.valueOf(betValue).intValue())) {
            jackBetResult.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
            return;
        }
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (Objects.isNull(gamePlayer) || gamePlayer.getGold() < betValue) {
            jackBetResult.code = Code.NOT_ENOUGH;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
            return;
        }
        gamePlayer.setGold(gamePlayer.getGold() - betValue);
        gameDataVo.getBaseBetInfo().merge(playerId, betValue, Long::sum);
        gameDataVo.getBaseBet().merge(playerId, betValue, Long::sum);
        jackBetResult.playerId = playerId;
        jackBetResult.betValue = betValue;
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(jackBetResult));
    }

    public void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum) {
        int time = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        int sendTime = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS);
        addPlayerTimer(new BlackJackProcessorHandler(nextExePlayer.getPlayerId(), gameDataVo.getId(), this),
                time + sendTime * sendCardNum);
    }

    /**
     * 处理分牌
     */
    private void dealCutCard(long playerId, ReqPokerSampleCardOperation req) {
        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(seatInfo) || seatInfo.getPlayerId() != playerId) {
            return;
        }
        NotifyBlackJackCutCard notify = new NotifyBlackJackCutCard();
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        //只能分一次牌并且只能在发牌时分牌
        if (seatInfo.getCurrentCards().size() != roomCfg.getHandPoker() || seatInfo.getCardIndex() != 0 || seatInfo.getCurrentCards().size() != 1) {
            notify.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));
            return;
        }
        List<Integer> cards = seatInfo.getCurrentCards();
        Map<Integer, PokerCard> cardListMap = BlackJackDataHelper.getCardListMap(BlackJackDataHelper.getPoolId(gameDataVo));
        int firstCard = cardListMap.get(cards.get(0)).getClientId();
        int secondCard = cardListMap.get(cards.get(1)).getClientId();
        //判断是否能分牌
        if (firstCard != secondCard) {
            notify.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));
            return;
        }
        seatInfo.setOperationType(req.type);
        //进行分牌
        Integer first = cards.remove(0);
        List<List<Integer>> totalCards = seatInfo.getCards();
        List<Integer> secondList = new ArrayList<>();
        secondList.add(first);
        totalCards.add(secondList);
        //通知分牌结果
        NotifyBlackJackCutCard notifyCutCard = new NotifyBlackJackCutCard();
        List<BlackJackCardInfo> list = new ArrayList<>(totalCards.size());
        for (List<Integer> handCards : totalCards) {
            BlackJackCardInfo blackJackCardInfo = new BlackJackCardInfo();
            blackJackCardInfo.cardIds = BlackJackDataHelper.getClientId(handCards, BlackJackDataHelper.getPoolId(gameDataVo));
            blackJackCardInfo.totalPoint = BlackJackDataHelper.getTotalPoint(cards);
            list.add(blackJackCardInfo);
        }
        notifyCutCard.playerId = playerId;
        notifyCutCard.cardInfoList = list;
        notifyCutCard.currentCardIds = 0;
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyCutCard));
    }


    /**
     * 处理拿牌
     */
    private void dealPutCard(long playerId, ReqPokerSampleCardOperation req) {
        PlayerSeatInfo currentPlayerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(currentPlayerSeatInfo) || playerId != currentPlayerSeatInfo.getPlayerId()) {
            return;
        }
        List<Integer> cards = currentPlayerSeatInfo.getCurrentCards();
        int card = getCard(gameDataVo);
        cards.add(card);
        //判断是否到21点和爆
        int sum = BlackJackDataHelper.getTotalPoint(cards);
        //21点和爆
        NotifyBlackJackPutCard notified = new NotifyBlackJackPutCard();
        notified.playerId = currentPlayerSeatInfo.getPlayerId();
        notified.cardId = BlackJackDataHelper.getClientCardId(gameDataVo, card);
        notified.totalPoint = sum;
        currentPlayerSeatInfo.setOperationType(req.type);
        PlayerSeatInfo nextExePlayer = currentPlayerSeatInfo;
        if (sum > BlackJackConstant.Common.PERFECT_POINT || cards.size() >= BlackJackConstant.Common.MAX_GET_CARD) {
            currentPlayerSeatInfo.setOver(true);
            //获取下一个玩家
            nextExePlayer = getNextExePlayer();
            if (Objects.isNull(nextExePlayer)) {
                notified.playerId = currentPlayerSeatInfo.getPlayerId();
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notified));
                startNextRoundOrSettlement();
                return;
            }
        }
        addNextTimer(nextExePlayer, 1);
        notified.playerId = currentPlayerSeatInfo.getPlayerId();
        notified.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        notified.operationId = nextExePlayer.getPlayerId();
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notified));
    }

    /**
     * 获取下一张牌
     */
    public int getCard(BlackJackGameDataVo gameDataVo) {
        List<Integer> cards = gameDataVo.getCards();
        if (cards.isEmpty()) {
            Map<Integer, PokerCard> cardListMap = BlackJackDataHelper.getCardListMap(BlackJackDataHelper.getPoolId(gameDataVo));
            gameDataVo.setCards(new ArrayList<>(cardListMap.keySet()));
            cards = gameDataVo.getCards();
            Collections.shuffle(cards);
            gameDataVo.setCards(cards);
        }
        return cards.remove(0);
    }


    /**
     * 处理停牌
     */
    public void dealStopCard(long playerId, int type) {
        NotifyPokerSampleCardOperation notify = new NotifyPokerSampleCardOperation();
        PlayerSeatInfo currentPlayerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(currentPlayerSeatInfo) || currentPlayerSeatInfo.getPlayerId() != playerId) {
            notify.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));
            return;
        }
        //设置操作类型
        currentPlayerSeatInfo.setOperationType(type);
        //玩家分牌停牌
        int cardNum = currentPlayerSeatInfo.getCards().size();
        if (cardNum > 1 && currentPlayerSeatInfo.getCardIndex() < cardNum - 1) {
            //玩家分牌停牌
            currentPlayerSeatInfo.setCardIndex(currentPlayerSeatInfo.getCardIndex() + 1);
            addNextTimer(currentPlayerSeatInfo, 0);
            //通知
            notify.operationType = type;
            notify.playerId = currentPlayerSeatInfo.getPlayerId();
            notify.nextPlayerId = currentPlayerSeatInfo.getPlayerId();
            notify.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
        } else {
            //本轮操作完成
            currentPlayerSeatInfo.setOver(true);
            //通知
            //获取下一个玩家
            PlayerSeatInfo nextExePlayer = getNextExePlayer();
            //通知
            notify.operationType = type;
            notify.playerId = currentPlayerSeatInfo.getPlayerId();
            if (Objects.nonNull(nextExePlayer)) {
                notify.nextPlayerId = nextExePlayer.getPlayerId();
                notify.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
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

    }

    @Override
    public EGameType gameControlType() {
        return EGameType.BLACK_JACK;
    }

    @Override
    public void initial() {

    }
}
