//package com.jjg.game.poker.game.blackjack.room;
//
//import com.jjg.game.common.proto.Pair;
//import com.jjg.game.core.constant.Code;
//import com.jjg.game.core.constant.EGameType;
//import com.jjg.game.core.data.PlayerController;
//import com.jjg.game.core.data.Room;
//import com.jjg.game.core.utils.PokerCardUtils;
//import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
//import com.jjg.game.poker.game.blackjack.gamephase.BlackJackSettlementPhase;
//import com.jjg.game.poker.game.blackjack.gamephase.BlackJackStartGamePhase;
//import com.jjg.game.poker.game.blackjack.gamephase.PlayerProcessorHandler;
//import com.jjg.game.poker.game.blackjack.message.resp.NotifyCutCard;
//import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
//import com.jjg.game.poker.game.common.BasePokerGameController;
//import com.jjg.game.poker.game.common.constant.PokerPhase;
//import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
//import com.jjg.game.poker.game.common.message.reps.NotifyPlayerOperate;
//import com.jjg.game.poker.game.common.message.req.ReqPlayerOperate;
//import com.jjg.game.room.constant.EGamePhase;
//import com.jjg.game.room.controller.AbstractRoomController;
//import com.jjg.game.room.data.room.GameDataVo;
//import com.jjg.game.room.data.room.GamePlayer;
//import com.jjg.game.room.message.RoomMessageBuilder;
//import com.jjg.game.room.sample.bean.Room_ChessCfg;
//
//import java.util.*;
//
//import static com.jjg.game.poker.game.blackjack.constant.BlackJackConstant.Common.WinPoint;
//
///**
// * @author lm
// * @date 2025/7/28 14:03
// */
//public class BlackJackGameController extends BasePokerGameController<BlackJackGameDataVo> {
//
//    public BlackJackGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
//        super(roomController);
//    }
//
//    @Override
//    public boolean canSeatDown() {
//        return false;
//    }
//
//
//    @Override
//    public void tryStartGame() {
//        addPokerPhaseTimer(new BlackJackStartGamePhase(this));
//    }
//
//    @Override
//    public void onPlayerJoinRoomAction() {
//        boolean canStartGame = gameDataVo.canStartGame();
//        if (canStartGame && getCurrentGamePhase() == EGamePhase.WAIT_READY) {
//            //尝试开启游戏
//            tryStartGame();
//        }
//        if (!canStartGame && getCurrentGamePhase() == EGamePhase.START_GAME) {
//            goBackWaitReadyPhase();
//        }
//    }
//
//    @Override
//    public void onPlayerLeaveRoomAction() {
//        boolean canStartGame = gameDataVo.canStartGame();
//        if (!canStartGame && getCurrentGamePhase() == EGamePhase.START_GAME) {
//            goBackWaitReadyPhase();
//        }
//    }
//
//    public NotifyPlayerOperate notifyStopCardResult(BlackJackGameController blackJackGameController, PlayerSeatInfo newInfo, long oldPlayerId) {
//        //通知
//        NotifyPlayerOperate notifyPlayerOperate = new NotifyPlayerOperate();
//        notifyPlayerOperate.playerId = oldPlayerId;
//        notifyPlayerOperate.operateType = BlackJackConstant.Operation.STOP_CARD;
//        return notifyNextPlayer(blackJackGameController, newInfo, notifyPlayerOperate);
//    }
//
//    public NotifyPlayerOperate notifyNextPlayer(BlackJackGameController blackJackGameController, PlayerSeatInfo newInfo, NotifyPlayerOperate notifyPlayerOperate) {
//        PlayerProcessorHandler handler = new PlayerProcessorHandler(newInfo.getPlayerId(), gameDataVo.getRound(), blackJackGameController);
//        Map<Integer, Integer> chessStageOrder = gameDataVo.getRoomCfg().getChessStageOrder();
//        blackJackGameController.addPlayerTimer(newInfo, handler, chessStageOrder.getOrDefault(PokerPhase.PLAY_CARDS.getValue(), 2000));
//        //通知下一个玩家
//        notifyPlayerOperate.nextPlayerId = newInfo.getPlayerId();
//        notifyPlayerOperate.endTime = newInfo.getPlayerGameTimerEvent().getNextTime();
//        return notifyPlayerOperate;
//    }
//
//    @Override
//    public void playerOperate(PlayerController playerController, ReqPlayerOperate reqPlayerOperate) {
//        PlayerSeatInfo currentPlayerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
//        if (playerController.playerId() != currentPlayerSeatInfo.getPlayerId()) {
//            log.error("非法请求 playerId:{} ", currentPlayerSeatInfo.getPlayerId());
//            return;
//        }
//        switch (reqPlayerOperate.operateType) {
//            case BlackJackConstant.Operation.BET -> dealBet(playerController.playerId(), reqPlayerOperate);
//            case BlackJackConstant.Operation.PUT_CARD -> dealPutCard();
//            case BlackJackConstant.Operation.CUT_CARD -> dealCutCard(playerController.playerId(), reqPlayerOperate);
//            case BlackJackConstant.Operation.STOP_CARD -> dealStopCard(playerController.playerId(), reqPlayerOperate);
//            default -> log.error("非法请求 playerId:{} ", currentPlayerSeatInfo.getPlayerId());
//        }
//    }
//
//    /**
//     * 处理分牌
//     */
//    private void dealCutCard(long playerId, ReqPlayerOperate reqPlayerOperate) {
//        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
//        NotifyPlayerOperate notifyPlayerOperate = new NotifyPlayerOperate();
//        //只能分一次牌并且只能在发牌时分牌
//        if (seatInfo.getCardIndex() != 0 || seatInfo.getCurrentCards().size() != 2) {
//            notifyPlayerOperate.code = Code.PARAM_ERROR;
//            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyPlayerOperate));
//            return;
//        }
//        List<Integer> cards = seatInfo.getCurrentCards();
//        int firstCard = PokerCardUtils.getPointId(cards.get(0).byteValue());
//        int secondCard = PokerCardUtils.getPointId(cards.get(1).byteValue());
//        //判断是否能分牌
//        if (firstCard != secondCard) {
//            notifyPlayerOperate.code = Code.PARAM_ERROR;
//            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyPlayerOperate));
//            return;
//        }
//        seatInfo.setOperationType(BlackJackConstant.Operation.CUT_CARD);
//        //进行分牌
//        Integer first = cards.remove(0);
//        List<List<Integer>> totalCards = seatInfo.getCards();
//        List<Integer> secondList = new ArrayList<>();
//        secondList.add(first);
//        totalCards.add(secondList);
//        //通知分牌结果
//        NotifyCutCard notifyCutCard = new NotifyCutCard();
//        notifyCutCard.playerId = playerId;
//        notifyCutCard.firstList = cards;
//        notifyCutCard.secondList = secondList;
//        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyCutCard));
//    }
//
//    /**
//     * 处理拿牌
//     */
//    private void dealPutCard() {
//        PlayerSeatInfo currentPlayerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
//        List<Integer> cards = currentPlayerSeatInfo.getCurrentCards();
//        int card = getCard(gameDataVo);
//        cards.add(card);
//        //判断是否到21点和爆
//        int sum = cards.stream().mapToInt(v -> (int) PokerCardUtils.getPointId(v.byteValue()))
//                .sum();
//        //21点和爆
//        PlayerSeatInfo newInfo = currentPlayerSeatInfo;
//        NotifyPlayerOperate notifyPlayerOperate = new NotifyPlayerOperate();
//        notifyPlayerOperate.playerId = currentPlayerSeatInfo.getPlayerId();
//        notifyPlayerOperate.operateType = BlackJackConstant.Operation.PUT_CARD;
//        notifyPlayerOperate.operateList = List.of(card);
//        if (sum >= WinPoint) {
//            currentPlayerSeatInfo.setOver(true);
//            if (canSettlement(gameDataVo)) {
//                broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPlayerOperate));
//                doSettlement();
//                return;
//            }
//            //获取下一个玩家
//            gameDataVo.setIndex(gameDataVo.getIndex() + 1);
//            newInfo = gameDataVo.getCurrentPlayerSeatInfo();
//        }
//        NotifyPlayerOperate notified = notifyNextPlayer(this, newInfo, notifyPlayerOperate);
//        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notified));
//    }
//
//    private int getCard(BlackJackGameDataVo gameDataVo) {
//        List<Integer> cards = gameDataVo.getCards();
//        if (cards.isEmpty()) {
//            cards = PokerCardUtils.getPokerIntIdExceptJoker();
//            Collections.shuffle(cards);
//            gameDataVo.setCards(cards);
//        }
//        return cards.remove(0);
//    }
//
//    /**
//     * 处理停牌
//     */
//    public void dealStopCard(long playerId, ReqPlayerOperate reqPlayerOperate) {
//        NotifyPlayerOperate notifyPlayerOperate = new NotifyPlayerOperate();
//        PlayerSeatInfo currentPlayerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
//        if (currentPlayerSeatInfo.getPlayerId() != playerId) {
//            notifyPlayerOperate.code = Code.PARAM_ERROR;
//            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyPlayerOperate));
//            return;
//        }
//        //设置操作类型
//        currentPlayerSeatInfo.setOperationType(BlackJackConstant.Operation.STOP_CARD);
//        if (currentPlayerSeatInfo.getPlayerGameTimerEvent() != null) {
//            timerCenter.remove(this, currentPlayerSeatInfo.getPlayerGameTimerEvent());
//            currentPlayerSeatInfo.setPlayerGameTimerEvent(null);
//        }
//        //玩家分牌停牌
//        int cardNum = currentPlayerSeatInfo.getCards().size();
//        if (cardNum > 1 && currentPlayerSeatInfo.getCardIndex() < cardNum - 1) {
//            //玩家分牌停牌
//            currentPlayerSeatInfo.setCardIndex(currentPlayerSeatInfo.getCardIndex() + 1);
//            //通知
//            notifyPlayerOperate = notifyStopCardResult(this, currentPlayerSeatInfo, playerId);
//            notifyPlayerOperate.operateList = List.of(currentPlayerSeatInfo.getCardIndex() - 1);
//            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPlayerOperate));
//            return;
//        } else {
//            //本轮操作完成
//            currentPlayerSeatInfo.setOver(true);
//        }
//        //判断是否可以进行结算
//        if (canSettlement(gameDataVo)) {
//            notifyPlayerOperate.playerId = playerId;
//            notifyPlayerOperate.operateType = BlackJackConstant.Operation.STOP_CARD;
//            notifyPlayerOperate.operateList = List.of(currentPlayerSeatInfo.getCardIndex() - 1);
//            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPlayerOperate));
//            doSettlement();
//            return;
//        }
//        //通知
//        //获取下一个玩家
//        gameDataVo.setIndex(gameDataVo.getIndex() + 1);
//        PlayerSeatInfo newInfo = gameDataVo.getCurrentPlayerSeatInfo();
//        notifyPlayerOperate = notifyStopCardResult(this, newInfo, playerId);
//        notifyPlayerOperate.operateList = List.of(currentPlayerSeatInfo.getCardIndex() - 1);
//        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPlayerOperate));
//
//    }
//
//    private void doSettlement() {
//        //如果这是最后一个执行 进行结算
//        addPokerPhaseTimer(new BlackJackSettlementPhase(this));
//    }
//
//    public boolean canSettlement(BlackJackGameDataVo gameDataVo) {
//        long count = gameDataVo.getPlayerSeatInfoList()
//                .stream().filter(s -> Objects.nonNull(s.getBetInfo()))
//                .count();
//        return gameDataVo.getIndex() >= count - 1;
//    }
//
//    /**
//     * 处理押注
//     */
//    private void dealBet(long playerId, ReqPlayerOperate reqPlayerOperate) {
//        NotifyPlayerOperate notifyPlayerOperate = new NotifyPlayerOperate();
//        List<Integer> operateList = reqPlayerOperate.operateList;
//        if (operateList == null || operateList.isEmpty()) {
//            notifyPlayerOperate.code = Code.PARAM_ERROR;
//            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyPlayerOperate));
//            return;
//        }
//        Integer betValue = operateList.get(0);
//        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
//        List<Integer> betList = roomCfg.getBetList();
//        if (betList == null || betList.isEmpty() || !betList.contains(betValue)) {
//            notifyPlayerOperate.code = Code.PARAM_ERROR;
//            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyPlayerOperate));
//            return;
//        }
//        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
//        gamePlayer.setGold(gamePlayer.getGold() - betValue);
//        //进行押注
//        PlayerSeatInfo playerSeatInfo = null;
//        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
//            if (info.getPlayerId() == playerId) {
//                playerSeatInfo = info;
//                break;
//            }
//        }
//        if (playerSeatInfo == null) {
//            notifyPlayerOperate.code = Code.PARAM_ERROR;
//            broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifyPlayerOperate));
//            return;
//        }
//        List<Integer> list = playerSeatInfo.getBetInfo().computeIfAbsent(BlackJackConstant.Operation.BET, b -> new ArrayList<>());
//        list.add(betValue);
//        notifyPlayerOperate.operateType = reqPlayerOperate.operateType;
//        notifyPlayerOperate.operateList = reqPlayerOperate.operateList;
//        notifyPlayerOperate.playerId = playerId;
//        broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPlayerOperate));
//    }
//
//    @Override
//    public void respRoomInitInfo(PlayerController playerController) {
//        //TODO
//    }
//
//    @Override
//    protected BlackJackGameDataVo copyRoomDataVo(GameDataVo<Room_ChessCfg> roomData) {
//        return new BlackJackGameDataVo(roomData.getRoomCfg());
//    }
//
//    @Override
//    public EGameType gameControlType() {
//        return EGameType.BLACK_JACK;
//    }
//}
