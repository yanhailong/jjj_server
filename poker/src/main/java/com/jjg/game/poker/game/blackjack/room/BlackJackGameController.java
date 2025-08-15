package com.jjg.game.poker.game.blackjack.room;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.blackjack.data.BlackJackBuilder;
import com.jjg.game.poker.game.blackjack.data.BlackJackDataHelper;
import com.jjg.game.poker.game.blackjack.gamephase.*;
import com.jjg.game.poker.game.blackjack.message.resp.*;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerSampleCardOperation;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.common.message.req.ReqPokerSampleCardOperation;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.BlackjackCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.*;

import static com.jjg.game.poker.game.common.constant.PokerConstant.PlayerOperation.*;


/**
 * @author lm
 * @date 2025/7/28 14:03
 */
@GameController(gameType = EGameType.BLACK_JACK)
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
        Long betValue = gameDataVo.getBaseBetInfo().get(playerId);
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (Objects.isNull(betValue) || Objects.isNull(gamePlayer) || gamePlayer.getGold() < betValue) {
            return;
        }
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        if (!(seatInfo.getCards().size() == 1 && seatInfo.getCurrentCards().size() == roomCfg.getHandPoker())) {
            return;
        }
        Map<Integer, Long> betInfo = gameDataVo.getAllBetInfo().computeIfAbsent(playerId, key -> new HashMap<>());
        betInfo.merge(seatInfo.getCardIndex(), betValue, Long::sum);
        int card = getCard(gameDataVo);
        seatInfo.getCurrentCards().add(card);
        seatInfo.setOperationType(req.type);
        NotifyBlackJackDoubleBetInfo notifyBlackJackDoubleBetInfo = new NotifyBlackJackDoubleBetInfo();
        notifyBlackJackDoubleBetInfo.betValue = betValue;
        //还有牌组
        if (seatInfo.getCardIndex() + 1 < seatInfo.getCards().size()) {
            addNextTimer(seatInfo, 0);
            notifyBlackJackDoubleBetInfo.putCardInfo = BlackJackBuilder.getNotifyBlackJackPutCard(playerId, seatInfo, gameDataVo, card);
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackDoubleBetInfo));
            return;
        }
        PlayerSeatInfo nextExePlayer = getNextExePlayer();
        if (Objects.nonNull(nextExePlayer)) {
            addNextTimer(nextExePlayer, 1);
            notifyBlackJackDoubleBetInfo.putCardInfo = BlackJackBuilder.getNotifyBlackJackPutCard(nextExePlayer.getPlayerId(), seatInfo, gameDataVo, card);
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackDoubleBetInfo));
        } else {
            notifyBlackJackDoubleBetInfo.putCardInfo = BlackJackBuilder.getNotifyBlackJackPutCard(0, seatInfo, gameDataVo, card);
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackDoubleBetInfo));
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
        if (gameDataVo.getAceBuyPlayerIds().contains(playerId)) {
            count += gameDataVo.getBaseBetInfo().getOrDefault(playerId, 0L);
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
        long count = betInfo.getOrDefault(index, 0L);
        if (gameDataVo.getAceBuyPlayerIds().contains(playerId)) {
            count += gameDataVo.getBaseBetInfo().getOrDefault(playerId, 0L);
        }
        return count;
    }

    /**
     * 处理购买ACE
     */
    private void dealBuyACE(long playerId, ReqPokerSampleCardOperation req) {
        NotifyBlackJackBetResult msg = new NotifyBlackJackBetResult();
        if (!gameDataVo.isCanBuyACE() || gameDataVo.getAceBuyPlayerIds().contains(playerId)) {
            return;
        }
        if (getCurrentGamePhase() != EGamePhase.PLAY_CART || gameDataVo.getAceBuyEndTime() == 0 || gameDataVo.getAceBuyEndTime() < System.currentTimeMillis()) {
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
        Long betValue = gameDataVo.getBaseBetInfo().getOrDefault(playerId, 0L);
        if (betValue == 0) {
            return;
        }
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
        if (Objects.isNull(gamePlayer)) {
            return;
        }
        if (gamePlayer.getGold() < betValue) {
            msg.code = Code.NOT_ENOUGH;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, msg));
            return;
        }
        gamePlayer.setGold(gamePlayer.getGold() - betValue);
        gameDataVo.getAceBuyPlayerIds().add(playerId);
        msg.playerId = playerId;
        msg.type = req.type;
        msg.betValue = betValue;
        //计算购买ace总金额
        long totalBet = 0;
        for (Long aceBuyPlayerId : gameDataVo.getAceBuyPlayerIds()) {
            totalBet += gameDataVo.getBaseBetInfo().getOrDefault(aceBuyPlayerId, 0L);
        }
        msg.totalBetValue = totalBet;
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(msg));
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
        for (PlayerSeatInfo seatInfo : gameDataVo.getPlayerSeatInfoList()) {
            if (!overSet.contains(seatInfo.getPlayerId())) {
                return false;
            }
        }
        return true;
    }

    public void notifyAceResult() {
        NotifyBlackJackBuyACE msg = new NotifyBlackJackBuyACE();
        //设置第一个开始的玩家 并添加定时
        PlayerSeatInfo first = gameDataVo.getPlayerSeatInfoList().get(gameDataVo.getIndex());
        addNextTimer(first, 0);
        msg.nextPlayerId = first.getPlayerId();
        msg.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        int totalPoint = BlackJackDataHelper.getTotalPoint(gameDataVo.getDealerCards());
        msg.result = totalPoint == BlackJackConstant.Common.PERFECT_POINT;
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(msg));
    }


    @Override
    public void tryStartGame() {
        if (gameDataVo.canStartGame() && getCurrentGamePhase() == EGamePhase.WAIT_READY) {
            addPokerPhaseTimer(new BlackJackBetPhase(this, gameDataVo.getId()));
        }
    }

    @Override
    public void dealBet(long playerId, ReqPokerBet reqPokerBet) {
        if (getCurrentGamePhase() != EGamePhase.BET) {
            return;
        }
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (Objects.isNull(gamePlayer)) {
            return;
        }
        //加入时间小于修正时间禁止下注
        if (gamePlayer.getPokerPlayerGameData().getJoinTime() + BlackJackConstant.Common.BET_FIX_TIME > gameDataVo.getPhaseEndTime()) {
            return;
        }
        NotifyBlackJackBetResult jackBetResult = new NotifyBlackJackBetResult();
        //进行押注
        SeatInfo mySeatInfo = null;
        for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
            if (seatInfo.getPlayerId() == playerId) {
                mySeatInfo = seatInfo;
                break;
            }
        }
        if (Objects.isNull(mySeatInfo)) {
            jackBetResult.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
            return;
        }
        BlackjackCfg blackjackCfg = BlackJackDataHelper.getBlackjackCfg(gameDataVo);
        List<Integer> betList = blackjackCfg.getBetList();
        long betValue = reqPokerBet.betValue;
        if (betList == null || betList.isEmpty() || !betList.contains(Long.valueOf(betValue).intValue())) {
            jackBetResult.code = Code.PARAM_ERROR;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
            return;
        }
        if (gamePlayer.getGold() < betValue) {
            jackBetResult.code = Code.NOT_ENOUGH;
            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, jackBetResult));
            return;
        }
        gamePlayer.setGold(gamePlayer.getGold() - betValue);
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        baseBetInfo.merge(playerId, betValue, Long::sum);
        Map<Integer, Long> betInfo = gameDataVo.getAllBetInfo().computeIfAbsent(playerId, key -> new HashMap<>());
        betInfo.merge(0, betValue, Long::sum);
        jackBetResult.playerId = playerId;
        jackBetResult.type = reqPokerBet.betType;
        jackBetResult.betValue = betValue;
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(jackBetResult));
        //如果全部押注完成 进入下一阶段
        if (isAllOver(baseBetInfo.keySet())) {
            removePokerPhaseTimer();
            BlackJackPlayCardPhase gamePhase = new BlackJackPlayCardPhase(this);
            addPokerPhase(gamePhase);
            //通知场上信息
            PokerBuilder.buildNotifyPhaseChange(EGamePhase.PLAY_CART, -1);
        }
    }

    public void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum) {
        int time = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        int sendTime = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS);
        addPlayerTimer(new BlackJackProcessorHandler(nextExePlayer.getPlayerId(), gameDataVo.getId(), this),
                time + sendTime * sendCardNum);
    }

    public void addNextTimer(PlayerSeatInfo nextExePlayer, int sendCardNum, int times) {
        int time = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.PLAY_CARDS);
        int sendTime = PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS);
        addPlayerTimer(new BlackJackProcessorHandler(nextExePlayer.getPlayerId(), gameDataVo.getId(), this),
                time + sendTime * sendCardNum + times);
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
        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(seatInfo) || seatInfo.getPlayerId() != playerId) {
            return;
        }
        Long betValue = gameDataVo.getBaseBetInfo().getOrDefault(playerId, 0L);
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (betValue <= 0 || Objects.isNull(gamePlayer)) {
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
        Integer first = cards.removeFirst();
        List<List<Integer>> totalCards = seatInfo.getCards();
        List<Integer> secondList = new ArrayList<>();
        secondList.add(first);
        totalCards.add(secondList);
        for (List<Integer> totalCard : totalCards) {
            totalCard.add(getCard(gameDataVo));
        }
        //下注
        gamePlayer.setGold(gamePlayer.getGold() - betValue);
        Map<Integer, Long> betInfo = gameDataVo.getAllBetInfo().computeIfAbsent(playerId, key -> new HashMap<>());
        betInfo.merge(seatInfo.getCardIndex() + 1, betValue, Long::sum);
        //修正500毫秒
        addNextTimer(seatInfo, 2, BlackJackConstant.Common.CUT_FIX_TIME);
        //通知分牌结果
        NotifyBlackJackCutCard notifyCutCard = new NotifyBlackJackCutCard();
        notifyCutCard.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        notifyCutCard.playerId = playerId;
        notifyCutCard.cardInfoList = BlackJackBuilder.getCardInfos(seatInfo, this);
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
        return cards.removeFirst();
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
            notify.operationType = type;
            notify.playerId = currentPlayerSeatInfo.getPlayerId();
            //获取下一个玩家
            PlayerSeatInfo nextExePlayer = getNextExePlayer();
            if (Objects.nonNull(nextExePlayer)) {
                addNextTimer(nextExePlayer, 0);
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
        RepsBlackJackRoomBaseInfo baseInfo = new RepsBlackJackRoomBaseInfo();
        baseInfo.settlementInfo = gameDataVo.getSettlementInfo();
        Map<Long, PlayerSeatInfo> playerSeatInfoMap = gameDataVo.getPlayerSeatInfoMap();
        baseInfo.playerInfos = new ArrayList<>();
        baseInfo.phase = getCurrentGamePhase();
        for (Map.Entry<Integer, SeatInfo> entry : gameDataVo.getSeatInfo().entrySet()) {
            SeatInfo seatInfo = entry.getValue();
            baseInfo.playerInfos.add(BlackJackBuilder.getBlackJackPlayerInfo(playerSeatInfoMap.get(seatInfo.getPlayerId()), seatInfo, this));
        }
        if (getCurrentGamePhase() == EGamePhase.PLAY_CART) {
            baseInfo.cardIds = new ArrayList<>(gameDataVo.getDealerCards().getFirst());
            if (gameDataVo.getAceBuyEndTime() == 0 || gameDataVo.getAceBuyEndTime() < System.currentTimeMillis()) {
                baseInfo.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
                baseInfo.operationId = gameDataVo.getCurrentPlayerSeatInfo().getPlayerId();
            }
        }
        BlackjackCfg blackjackCfg = BlackJackDataHelper.getBlackjackCfg(gameDataVo);
        baseInfo.chipsList = blackjackCfg.getBetList();
        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerController.playerId(), baseInfo));
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.BLACK_JACK;
    }

    @Override
    public void initial() {

    }
}
