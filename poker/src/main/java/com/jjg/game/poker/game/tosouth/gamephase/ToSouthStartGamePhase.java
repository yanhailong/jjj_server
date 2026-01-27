package com.jjg.game.poker.game.tosouth.gamephase;

import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BaseStartGamePhase;
import com.jjg.game.poker.game.tosouth.data.ToSouthDataHelper;
import com.jjg.game.poker.game.tosouth.room.ToSouthGameController;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthGameDataVo;
import com.jjg.game.poker.game.tosouth.util.ToSouthHandUtils;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 南方前进开始游戏阶段 (洗牌发牌动画)
 */
public class ToSouthStartGamePhase extends BaseStartGamePhase<ToSouthGameDataVo> {
    
    private final List<PlayerSeatInfo> instantWinners = new ArrayList<>();

    public ToSouthStartGamePhase(AbstractPhaseGameController<Room_ChessCfg, ToSouthGameDataVo> gameController, long executionGameId) {
        super(gameController, executionGameId);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        if (gameController instanceof ToSouthGameController controller) {
            ToSouthGameDataVo gameDataVo = controller.getGameDataVo();
            
            // 1. 洗牌发牌
            Map<Integer, PokerCard> cardListMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));
            sendCards(cardListMap, gameDataVo);

            // 2. 确定首出玩家 (黑桃3)
            PlayerSeatInfo playerSeatInfo = findSeatWithSpecifyCard(gameDataVo, new Card(0, 3).getValue());
            if (playerSeatInfo == null) {
                log.warn("南方前进牌组中没有黑桃3，请检查配置");
                // 容错：默认第一个人
                if (!gameDataVo.getPlayerSeatInfoList().isEmpty()) {
                    playerSeatInfo = gameDataVo.getPlayerSeatInfoList().get(0);
                }
            }
            
            if (playerSeatInfo != null) {
                gameDataVo.setIndex(playerSeatInfo.getSeatId());
                gameDataVo.setFirstRound(true);
                gameDataVo.setRoundLeaderSeatId(playerSeatInfo.getSeatId());
            }

            // 3. 检查通杀
            checkInstantWin(controller);
        }
    }

    private void sendCards(Map<Integer, PokerCard> cardListMap, BasePokerGameDataVo gameDataVo) {
        List<Integer> list = new ArrayList<>(cardListMap.keySet());
        Collections.shuffle(list);
        gameDataVo.setCards(list);

        List<Integer> cards = gameDataVo.getCards();
        int handPoker = gameDataVo.getRoomCfg().getHandPoker();

        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.isDelState()) {
                continue;
            }
            List<Integer> playCard = new ArrayList<>();
            for (int i = 0; i < handPoker; i++) {
                if (!cards.isEmpty()) {
                    playCard.add(cards.removeFirst());
                }
            }
            info.setCards(new ArrayList<>());
            info.getCards().add(playCard);
        }

        // 发送发牌通知
        if (gameController instanceof BasePokerGameController) {
             for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
                 gameController.respRoomInitInfo(gameController.getRoomController().getPlayerController(info.getPlayerId()));
             }
        }
    }

    private PlayerSeatInfo findSeatWithSpecifyCard(ToSouthGameDataVo gameDataVo, int specifyCard) {
        for (PlayerSeatInfo playerSeatInfo : gameDataVo.getPlayerSeatInfoList()) {
            for (Integer currentCard : playerSeatInfo.getCurrentCards()) {
                if (currentCard == specifyCard) {
                    return playerSeatInfo;
                }
            }
        }
        return null;
    }

    private void checkInstantWin(ToSouthGameController controller) {
        ToSouthGameDataVo gameDataVo = controller.getGameDataVo();
        Map<Integer, PokerCard> cardMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));

        instantWinners.clear();
        for (PlayerSeatInfo seatInfo : gameDataVo.getPlayerSeatInfoList()) {
            List<Integer> handCardIds = seatInfo.getCurrentCards();
            List<Card> handCards = handCardIds.stream().map(cardMap::get).collect(Collectors.toList());
            if (ToSouthHandUtils.checkInstantWin(handCards)) {
                instantWinners.add(seatInfo);
                log.info("玩家 {} 触发通杀！", seatInfo.getPlayerId());
            }
        }
    }

    @Override
    public void nextPhase() {
        if (gameController instanceof ToSouthGameController controller) {
            if (!instantWinners.isEmpty()) {
                // 通杀，直接进入结算
                controller.addPokerPhaseTimer(new ToSouthSettlementPhase(controller, instantWinners));
            } else {
                // 进入打牌阶段
                controller.addPokerPhaseTimer(new ToSouthPlayCardPhase(controller));
            }
        }
    }

    @Override
    public int getPhaseRunTime() {
        return 5000; // 5秒洗牌发牌动画时间
    }
}
