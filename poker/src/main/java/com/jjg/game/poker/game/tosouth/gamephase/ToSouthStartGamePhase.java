package com.jjg.game.poker.game.tosouth.gamephase;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.gamephase.BaseStartGamePhase;
import com.jjg.game.poker.game.tosouth.data.ToSouthDataHelper;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;
import com.jjg.game.poker.game.tosouth.data.ToSouthSettlementContext;
import com.jjg.game.poker.game.tosouth.manager.ToSouthStartManager;
import com.jjg.game.poker.game.tosouth.message.resp.RespToSouthSendCardsInfo;
import com.jjg.game.poker.game.tosouth.room.ToSouthGameController;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthGameDataVo;
import com.jjg.game.poker.game.tosouth.util.ToSouthHandUtils;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.RANK_2;
import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.RANK_3;
import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.SPADE_SUITS;

/**
 * 南方前进开始游戏阶段 (洗牌发牌动画)
 */
public class ToSouthStartGamePhase extends BaseStartGamePhase<ToSouthGameDataVo> {
    
    /** 炸弹测试模式：true=每人发满手炸弹（用于验证炸弹结算逻辑），false=正常随机发牌 */
    private static final boolean BOMB_TEST_MODE = false;

    private ToSouthSettlementContext instantWinContext;

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
            if (BOMB_TEST_MODE) {
                log.info("炸弹测试模式已开启，跳过随机发牌，改为发炸弹牌");
                sendBombTestCards(cardListMap, gameDataVo);
            } else {
                sendCards(cardListMap, gameDataVo);
            }

            // 2. 确定首出玩家 (黑桃3)
            PlayerSeatInfo playerSeatInfo = findSeatWithSpecifyCard(gameDataVo, cardListMap, RANK_3, SPADE_SUITS);
            if (playerSeatInfo == null) {
                log.warn("南方前进牌组中没有黑桃3，请检查配置");
                return;
            }

            gameDataVo.setIndex(playerSeatInfo.getSeatId());
            gameDataVo.setRoundLeaderSeatId(playerSeatInfo.getSeatId());

            // 3. 检查通杀（炸弹测试模式下跳过，否则人人有炸弹把把触发通杀）
            if (!BOMB_TEST_MODE) {
                checkInstantWin(controller);
            }
        }
    }

    private void sendCards(Map<Integer, PokerCard> cardListMap, ToSouthGameDataVo gameDataVo) {
        List<Integer> list = new ArrayList<>(cardListMap.keySet());

        // ====== GM发牌处理：预分配GM指定的手牌，并从牌池中移除 ======
        Map<Long, List<Integer>> gmPreAssigned = new HashMap<>();
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.isDelState()) {
                continue;
            }
            List<int[]> gmCards = ToSouthStartManager.consumeGmCards(info.getPlayerId());
            if (gmCards == null || gmCards.isEmpty()) {
                continue;
            }
            List<Integer> preAssignedIds = new ArrayList<>();
            for (int[] spec : gmCards) {
                int suit = spec[0];
                int rank = spec[1];
                Integer matchedId = findCardIdBySuitRank(cardListMap, suit, rank, list);
                if (matchedId != null) {
                    preAssignedIds.add(matchedId);
                    list.remove(matchedId);
                } else {
                    log.warn("GM发牌 - 玩家: {}, 未找到匹配的牌: suit={}, rank={}", info.getPlayerId(), suit, rank);
                }
            }
            if (!preAssignedIds.isEmpty()) {
                gmPreAssigned.put(info.getPlayerId(), preAssignedIds);
                log.info("GM发牌 - 玩家: {}, 预分配手牌数: {}", info.getPlayerId(), preAssignedIds.size());
            }
        }
        // ====== GM发牌处理结束 ======

        Collections.shuffle(list);
        gameDataVo.setCards(list);

        List<Integer> cards = gameDataVo.getCards();
        int handPoker = gameDataVo.getRoomCfg().getHandPoker();
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.isDelState()) {
                continue;
            }

            List<Integer> playCard = new ArrayList<>();

            // GM预分配的手牌优先加入
            List<Integer> preAssigned = gmPreAssigned.get(info.getPlayerId());
            if (preAssigned != null) {
                playCard.addAll(preAssigned);
            }

            // 剩余手牌从洗好的牌堆中补全
            int remaining = handPoker - playCard.size();
            for (int i = 0; i < remaining; i++) {
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
            List<Integer> sortedHandCards = new ArrayList<>();
            for (Card c : handCards) {
                if (c instanceof PokerCard pc) {
                    playCard.add(pc.getPokerPoolId());
                    sortedHandCards.add(pc.getClientId());
                }
            }

            info.setCards(new ArrayList<>());
            info.getCards().add(playCard);

            if (log.isDebugEnabled()) {
                 // 此时 playCard 已经排序
                log.debug("发牌 - 玩家: {}, 座位: {}, 手牌: {}{}", info.getPlayerId(), info.getSeatId(),
                        gmPreAssigned.containsKey(info.getPlayerId()) ? "[GM] " : "",
                        ToSouthHandUtils.cardListToString(handCards));
            }

            RespToSouthSendCardsInfo sendCardsInfo = new RespToSouthSendCardsInfo();
            sendCardsInfo.sortedHandCards = sortedHandCards;
            // 简单打乱处理
            List<Integer> temp = new ArrayList<>(sortedHandCards);
            Collections.shuffle(temp);
            sendCardsInfo.originalHandCards = temp;
            sendCardsInfo.highlightCards = highlightIds;
            gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), sendCardsInfo));
        }
    }

    /**
     * 根据花色和点数从牌池中查找匹配的牌ID (pokerPoolId)
     *
     * @param cardListMap  牌池映射
     * @param suit         花色
     * @param rank         点数
     * @param availableIds 当前可用的牌ID列表
     * @return 匹配的pokerPoolId，未找到返回null
     */
    private Integer findCardIdBySuitRank(Map<Integer, PokerCard> cardListMap, int suit, int rank, List<Integer> availableIds) {
        for (Integer id : availableIds) {
            PokerCard card = cardListMap.get(id);
            if (card != null && card.getSuit() == suit && card.getRank() == rank) {
                return id;
            }
        }
        return null;
    }

    /**
     * 炸弹测试模式发牌：
     * 将3~A各花色组成的四张同点炸弹按轮次分配给玩家，剩余手牌位置用2填充。
     * 以4人桌为例：每人获得3个炸弹(12张) + 1张2 = 13张。
     */
    private void sendBombTestCards(Map<Integer, PokerCard> cardListMap, ToSouthGameDataVo gameDataVo) {
        // 按点数分组，2单独收集作为填充牌
        Map<Integer, List<PokerCard>> rankGroups = new HashMap<>();
        List<PokerCard> deuces = new ArrayList<>();
        for (PokerCard card : cardListMap.values()) {
            if (card.getRank() == RANK_2) {
                deuces.add(card);
            } else {
                rankGroups.computeIfAbsent(card.getRank(), k -> new ArrayList<>()).add(card);
            }
        }

        // 收集完整的四张同点炸弹组，按点数升序排列保证稳定性（3最小优先分配）
        List<List<PokerCard>> bombGroups = rankGroups.entrySet().stream()
                .filter(e -> e.getValue().size() >= 4)
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ArrayList<>(e.getValue().subList(0, 4)))
                .collect(Collectors.toList());

        List<PlayerSeatInfo> activePlayers = gameDataVo.getPlayerSeatInfoList().stream()
                .filter(p -> !p.isDelState())
                .collect(Collectors.toList());
        int playerCount = activePlayers.size();
        int handPoker = gameDataVo.getRoomCfg().getHandPoker();

        // 炸弹组按轮次分配给玩家（手牌未满才追加）
        Map<Long, List<PokerCard>> playerCardMap = new HashMap<>();
        for (PlayerSeatInfo info : activePlayers) {
            playerCardMap.put(info.getPlayerId(), new ArrayList<>());
        }
        for (int i = 0; i < bombGroups.size(); i++) {
            PlayerSeatInfo player = activePlayers.get(i % playerCount);
            List<PokerCard> hand = playerCardMap.get(player.getPlayerId());
            if (hand.size() + 4 <= handPoker) {
                hand.addAll(bombGroups.get(i));
            }
        }

        // 剩余位置用2补齐
        int deuceIdx = 0;
        for (PlayerSeatInfo info : activePlayers) {
            List<PokerCard> hand = playerCardMap.get(info.getPlayerId());
            while (hand.size() < handPoker && deuceIdx < deuces.size()) {
                hand.add(deuces.get(deuceIdx++));
            }
        }

        // 炸弹测试后牌堆为空
        gameDataVo.setCards(new ArrayList<>());

        // 与 sendCards 保持相同的发牌协议
        for (PlayerSeatInfo info : activePlayers) {
            List<Card> handCards = new ArrayList<>(playerCardMap.get(info.getPlayerId()));
            // 按规则排列手牌并计算高亮牌
            List<Integer> highlightIds = ToSouthHandUtils.sortAndGetHighlightCards(handCards);
            if (!highlightIds.isEmpty()) {
                gameDataVo.getPlayerHighlightCards().put(info.getPlayerId(), highlightIds);
            }

            List<Integer> playCard = new ArrayList<>();
            List<Integer> sortedHandCards = new ArrayList<>();
            for (Card c : handCards) {
                if (c instanceof PokerCard pc) {
                    playCard.add(pc.getPokerPoolId());
                    sortedHandCards.add(pc.getClientId());
                }
            }

            info.setCards(new ArrayList<>());
            info.getCards().add(playCard);

            if (log.isDebugEnabled()) {
                log.debug("炸弹测试发牌 - 玩家: {}, 座位: {}, 手牌: {}", info.getPlayerId(), info.getSeatId(),
                        ToSouthHandUtils.cardListToString(handCards));
            }

            RespToSouthSendCardsInfo sendCardsInfo = new RespToSouthSendCardsInfo();
            sendCardsInfo.sortedHandCards = sortedHandCards;
            List<Integer> temp = new ArrayList<>(sortedHandCards);
            Collections.shuffle(temp);
            sendCardsInfo.originalHandCards = temp;
            sendCardsInfo.highlightCards = highlightIds;
            gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(info.getPlayerId(), sendCardsInfo));
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

        // key: PlayerSeatInfo, value: 该玩家自己的通杀牌型信息
        Map<PlayerSeatInfo, Pair<Integer, List<Integer>>> winnerMap = new LinkedHashMap<>();

        for (PlayerSeatInfo seatInfo : gameDataVo.getPlayerSeatInfoList()) {
            List<Integer> handCardIds = seatInfo.getCurrentCards();
            List<Card> handCards = handCardIds.stream().map(cardMap::get).collect(Collectors.toList());
            if (log.isDebugEnabled()) {
                List<Card> sorted = new ArrayList<>(handCards);
                sorted.sort(ToSouthHandUtils.CARD_COMPARATOR);
                log.debug("通杀检查 - 玩家: {}, 手牌: {}", seatInfo.getPlayerId(), ToSouthHandUtils.cardListToString(sorted));
            }
            Pair<Integer, List<Integer>> instantWinCards = ToSouthHandUtils.getInstantWinCards(handCards);
            if (instantWinCards != null) {
                winnerMap.put(seatInfo, instantWinCards);
                log.info("玩家 {} 触发通杀！类型: {}", seatInfo.getPlayerId(), instantWinCards.getFirst());
            }
        }

        if (!winnerMap.isEmpty()) {
            instantWinContext = new ToSouthSettlementContext();
            instantWinContext.setInstantWin(true);
            for (Map.Entry<PlayerSeatInfo, Pair<Integer, List<Integer>>> entry : winnerMap.entrySet()) {
                Pair<Integer, List<Integer>> winCards = entry.getValue();
                instantWinContext.addItem(new ToSouthSettlementContext.SettlementItem(
                        entry.getKey(),
                        true,
                        winCards.getFirst(),
                        winCards.getSecond()
                ));
            }
        }
    }

    @Override
    public void nextPhase() {
        if (gameController instanceof ToSouthGameController controller) {
            if (instantWinContext != null && !instantWinContext.getWinners().isEmpty()) {
                // 通杀，直接进入结算
                controller.addPokerPhaseTimer(new ToSouthSettlementPhase(controller, instantWinContext));
            } else {
                // 进入打牌阶段
                controller.addPokerPhaseTimer(new ToSouthPlayCardPhase(controller));
            }
        }
    }
}
