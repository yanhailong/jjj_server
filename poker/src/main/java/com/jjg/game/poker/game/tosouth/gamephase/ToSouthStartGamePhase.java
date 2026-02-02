package com.jjg.game.poker.game.tosouth.gamephase;

import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.gamephase.BaseStartGamePhase;
import com.jjg.game.poker.game.tosouth.data.ToSouthDataHelper;
import com.jjg.game.poker.game.tosouth.room.ToSouthGameController;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthGameDataVo;
import com.jjg.game.poker.game.tosouth.util.ToSouthHandUtils;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;

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
            
            // 确保 playerSeatInfoList 已初始化
            if (gameDataVo.getPlayerSeatInfoList().isEmpty()) {
                controller.genPlayerSeatInfoList(gameDataVo.getSeatInfo(), gameDataVo.getPlayerSeatInfoList());
                log.info("初始化玩家列表完成，人数: {}", gameDataVo.getPlayerSeatInfoList().size());
            }
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(controller.getRoom().getRoomCfgId());
            gameDataVo.setRoomBet(warehouseCfg.getEnterLimit());
            log.debug("南方前进开始游戏，房间底注为：{}", warehouseCfg.getEnterLimit());
            // 1. 洗牌发牌
            Map<Integer, PokerCard> cardListMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));
            sendCards(cardListMap, gameDataVo);

            // 2. 确定首出玩家 (黑桃3)
            PlayerSeatInfo playerSeatInfo = findSeatWithSpecifyCard(gameDataVo, cardListMap, 3, 4);
            if (playerSeatInfo == null) {
                log.warn("南方前进牌组中没有黑桃3，请检查配置");
                return;
            }

            gameDataVo.setIndex(playerSeatInfo.getSeatId());
            gameDataVo.setRoundLeaderSeatId(playerSeatInfo.getSeatId());

            // 3. 检查通杀
            checkInstantWin(controller);
        }
    }

    private void sendCards(Map<Integer, PokerCard> cardListMap, ToSouthGameDataVo gameDataVo) {
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

            List<Card> handCards = new ArrayList<>();
            for (Integer id : playCard) {
                handCards.add(cardListMap.get(id));
            }
            // 按照 炸弹 > 连对 > 顺子 > 三条 > 对子 > 单张 的规则排列手牌，并计算高亮牌(2,炸弹，连对)
            List<Integer> highlightIds = ToSouthHandUtils.sortAndGetHighlightCards(handCards);
            
            if (!highlightIds.isEmpty()) {
                gameDataVo.getPlayerHighlightCards().put(info.getPlayerId(), highlightIds);
            }

            playCard.clear();
            for (Card c : handCards) {
                if (c instanceof PokerCard pc) {
                    playCard.add(pc.getPokerPoolId());
                }
            }

            info.setCards(new ArrayList<>());
            info.getCards().add(playCard);
            
            if (log.isDebugEnabled()) {
                 // 此时 playCard 已经排序
                 List<Card> hand = playCard.stream().map(cardListMap::get).collect(Collectors.toList());
                log.debug("发牌 - 玩家: {}, 座位: {}, 手牌: {}, 高亮: {}", info.getPlayerId(), info.getSeatId(), ToSouthHandUtils.cardListToString(hand), highlightIds);
            }
        }

        // 发送发牌通知
        if (gameController instanceof BasePokerGameController) {
             for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
                 gameController.respRoomInitInfo(gameController.getRoomController().getPlayerController(info.getPlayerId()));
             }
        }
    }

    private PlayerSeatInfo findSeatWithSpecifyCard(ToSouthGameDataVo gameDataVo, Map<Integer, PokerCard> cardMap, int rank, int suit) {
        for (PlayerSeatInfo playerSeatInfo : gameDataVo.getPlayerSeatInfoList()) {
            for (Integer cardId : playerSeatInfo.getCurrentCards()) {
                PokerCard pokerCard = cardMap.get(cardId);
                if (pokerCard != null && pokerCard.getRank() == rank && pokerCard.getSuit() == suit) {
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
            if (log.isDebugEnabled()) {
                List<Card> sorted = new ArrayList<>(handCards);
                sorted.sort(ToSouthHandUtils.CARD_COMPARATOR);
                log.debug("通杀检查 - 玩家: {}, 手牌: {}", seatInfo.getPlayerId(), ToSouthHandUtils.cardListToString(sorted));
            }
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
}
