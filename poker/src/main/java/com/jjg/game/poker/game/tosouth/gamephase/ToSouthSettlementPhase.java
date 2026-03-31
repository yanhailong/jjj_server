package com.jjg.game.poker.game.tosouth.gamephase;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.Card;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.gamephase.BaseSettlementPhase;
import com.jjg.game.poker.game.tosouth.data.ToSouthDataHelper;
import com.jjg.game.poker.game.tosouth.data.ToSouthSettlementContext;
import com.jjg.game.poker.game.tosouth.message.bean.ToSouthPlayerSettlementInfo;
import com.jjg.game.poker.game.tosouth.message.notify.NotifyToSouthSettlementInfo;
import com.jjg.game.poker.game.tosouth.room.ToSouthGameController;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthGameDataVo;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthGameLog;
import com.jjg.game.poker.game.tosouth.util.ToSouthHandUtils;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.SouthernMoneyCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 南方前进结算阶段
 */
public class ToSouthSettlementPhase extends BaseSettlementPhase<ToSouthGameDataVo> {
    private static final Logger log = LoggerFactory.getLogger(ToSouthSettlementPhase.class);
    private final ToSouthSettlementContext context;
    private final List<PlayerSeatInfo> winners;

    public ToSouthSettlementPhase(AbstractPhaseGameController<Room_ChessCfg, ToSouthGameDataVo> gameController, ToSouthSettlementContext context) {
        super(gameController);
        this.context = context;
        this.winners = context.getWinners();
    }

    @Override
    public void phaseFinishDoAction() {
        if (!(gameController instanceof ToSouthGameController controller)) {
            return;
        }
        // 结算结束后，踢出离线的真实玩家，避免影响下一局
        List<GamePlayer> players = new ArrayList<>(controller.getGameDataVo().getGamePlayerMap().values());
        for (GamePlayer gamePlayer : players) {
            if (gamePlayer instanceof GameRobotPlayer) {
                continue;
            }
            long playerId = gamePlayer.getId();
            RoomPlayer roomPlayer = controller.getRoom().getRoomPlayers().get(playerId);
            if (roomPlayer != null && !roomPlayer.isOnline()) {
                log.info("南方前进结算后：玩家 {} 离线，踢出房间", playerId);
                controller.getRoomController().getRoomManager().exitRoom(playerId);
            }
        }
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        if (gameController instanceof ToSouthGameController controller) {
            ToSouthGameDataVo gameDataVo = controller.getGameDataVo();
            SouthernMoneyCfg moneyCfg = ToSouthDataHelper.getSouthernMoneyCfg(gameDataVo);
            if (moneyCfg == null) {
                log.error("缺少SouthernMoneyCfg配置: {}", gameDataVo.getRoomCfg().getId());
                return;
            }

            Map<Long, Long> settlementMap = new HashMap<>(); // playerId -> score change
            long baseBet = getBaseBet(gameDataVo); // 获取房间底注

            calSettlement(gameDataVo, settlementMap, baseBet, moneyCfg);

            // 记录本局赢家，供下局判断首出玩家
            if (!winners.isEmpty()) {
                gameDataVo.setLastGameWinnerPlayerId(winners.getFirst().getPlayerId());
            }

            // 应用结算结果
            long totalTax = 0;
            List<ToSouthPlayerSettlementInfo> playerSettlementInfos = new ArrayList<>();

            Map<Long, Long> settlementMap2 = new HashMap<>(settlementMap);
            //重新计算结算
            for (Map.Entry<Long, Long> entry : settlementMap.entrySet()) {
                long playerId = entry.getKey();
                long change = entry.getValue();
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
                if (change < 0) {
                    long loseAmount = -change;
                    int transactionItemId = controller.getGameTransactionItemId();
                    int goldCfgId = ItemUtils.getGoldItemId();
                    int diamondCfgId = ItemUtils.getDiamondItemId();
                    Map<Long, Long> positiveMap = settlementMap.entrySet().stream()
                            .filter(entry2 -> entry2.getValue() != null && entry2.getValue() > 0)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    if (transactionItemId == goldCfgId) {
                        long gold = gamePlayer.getGold();
                        if (gold < loseAmount) {
                            //只有三家的时候才会复现
                            long l = gold / positiveMap.size();
                            long l1 = loseAmount / positiveMap.size();
                            settlementMap2.put(playerId, -gold);
                            positiveMap.forEach((k, v) -> {
                                settlementMap2.put(k,v-l1+l);
                                log.info("111111111111111:{},{},{}",l,l1,(v) - l1);
                            });

                        }
                    } else if (transactionItemId == diamondCfgId) {
                        long diamond = gamePlayer.getDiamond();
                        if (diamond < loseAmount) {
                            long l = diamond / positiveMap.size();
//                          18000 - 26000 = -8000
                            long l1 = l - loseAmount;
                            settlementMap2.put(playerId, diamond);
                            positiveMap.forEach((k, v) -> {
//                                26000 - 8000
                                settlementMap2.put(k,v-l1+l);
                            });
                        }
                    }
                }
            }

            for (Map.Entry<Long, Long> entry : settlementMap2.entrySet()) {
                long playerId = entry.getKey();
                long change = entry.getValue();
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
                if (gamePlayer == null) {
                    log.error("南方前进结算时 gamePlayer=null playerId:{}", playerId);
                    continue;
                }

                long finalWinScore = change;

                if (change > 0) {
                    // 扣除抽水
                    long tax = BigDecimal.valueOf(change)
                            .multiply(BigDecimal.valueOf(10000 - gameDataVo.getRoomCfg().getEffectiveRatio()))
                            .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN).longValue();

                    totalTax += tax;
                    finalWinScore = change - tax;
                    controller.addItem(playerId, finalWinScore, AddType.GAME_SETTLEMENT);
                    gameDataTracker.addGameLogData("tax", totalTax);
                    if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
                        robotPlayer.setLastWin(1);
                    } else {
                        controller.dealIncome(gamePlayer, finalWinScore);
                    }
                } else {
                    long loseAmount = -change;

                    if (loseAmount > 0) {
                        controller.deductItem(playerId, loseAmount, AddType.GAME_SETTLEMENT, "南方前进输钱", false);
                    }

                    if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
                        robotPlayer.setLastWin(2);
                    } else {
                        controller.dealIncome(gamePlayer, change);
                    }

                }


                // 构建玩家结算信息
                ToSouthPlayerSettlementInfo info = new ToSouthPlayerSettlementInfo();
                info.playerId = playerId;
                info.winScore = finalWinScore;
                info.currentScore = controller.getTransactionItemNum(playerId);

                PlayerSeatInfo seatInfo = gameDataVo.getPlayerSeatInfoMap().get(playerId);
                if (seatInfo != null) {
                    info.handCards = PokerDataHelper.getClientId(gameDataVo, seatInfo.getCurrentCards());
                    info.isWinner = winners.contains(seatInfo);
                    // 检查是否通杀
                    if (context.isInstantWin()) {
                        for (ToSouthSettlementContext.SettlementItem item : context.getSettlementItems()) {
                            if (item.seatInfo.getPlayerId() == playerId) {
                                info.isInstantWin = true;
                                info.instantWinCards = item.instantWinCards;
                                info.instantWinType = item.instantWinType;
                                break;
                            }
                        }
                    }
                }
                playerSettlementInfos.add(info);
            }

            // 发送结算消息给客户端
            NotifyToSouthSettlementInfo notify = new NotifyToSouthSettlementInfo();
            notify.settlementInfos = playerSettlementInfos;
            notify.endTime = System.currentTimeMillis();
            controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
            log.info("南方前进结算map: {}", settlementMap2);

            // ========== 记录最终结算到一局日志，并打印流程日志和结算日志 ==========
            ToSouthGameLog gameLog = gameDataVo.getGameLog();
            Map<Integer, PokerCard> cardMapForLog = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));
            for (ToSouthPlayerSettlementInfo sInfo : playerSettlementInfos) {
                // 计算剩余手牌数
                PlayerSeatInfo seat = gameDataVo.getPlayerSeatInfoMap().get(sInfo.playerId);
                int remainCards = seat != null ? seat.getCurrentCards().size() : 0;

                // 构建结算明细描述
                String detail = "";
                if (!sInfo.isWinner && !context.isInstantWin() && seat != null) {
                    List<Card> handCards = seat.getCurrentCards().stream().map(cardMapForLog::get).collect(Collectors.toList());
                    int cardCount = handCards.size();
                    int countTwo = ToSouthHandUtils.countTwo(handCards);
                    int countRedTwo = ToSouthHandUtils.countRedTwo(handCards);
                    int countBlackTwo = countTwo - countRedTwo;
                    int cardMulti = (cardCount == 13) ? cardCount * 2 : cardCount;
                    int redTwoMulti = moneyCfg.getRemainred2();
                    int blackTwoMulti = moneyCfg.getRemainblack2();
                    int optimalBombMulti = ToSouthHandUtils.calcOptimalBombMultiplier(
                            handCards, moneyCfg.getFourkindboom1(), moneyCfg.getRemainBoom1(), moneyCfg.getFourpairsboom1());
                    int totalMulti = cardMulti + countRedTwo * redTwoMulti + countBlackTwo * blackTwoMulti + optimalBombMulti;
                    detail = String.format("牌倍:%d, 红2:%dx%d, 黑2:%dx%d, 炸弹倍:%d, 总倍数:%d",
                            cardMulti, countRedTwo, redTwoMulti, countBlackTwo, blackTwoMulti, optimalBombMulti, totalMulti);
                } else if (!sInfo.isWinner && context.isInstantWin() && seat != null) {
                    int cardCount = seat.getCurrentCards().size();
                    detail = String.format("通杀翻倍, 总倍数:%d", cardCount * 2);
                }
                gameLog.recordFinalSettlement(sInfo.playerId, sInfo.winScore, sInfo.isWinner, remainCards, detail);
            }

            // 打印流程日志和结算日志
            String roomInfo = "房间:" + gameDataVo.getRoomCfg().getId() + " 底注:" + baseBet;
            log.info(gameLog.buildFlowLog(roomInfo));
            log.info(gameLog.buildSettlementLog(roomInfo));
        }
    }

    private long getBaseBet(ToSouthGameDataVo gameDataVo) {
        return gameDataVo.getRoomBet(); // Placeholder
    }

    private void calSettlement(ToSouthGameDataVo gameDataVo, Map<Long, Long> settlementMap, long baseBet, SouthernMoneyCfg moneyCfg) {
        if (CollUtil.isEmpty(winners)) {
            log.error("结算没有赢家，请检查出牌逻辑！");
            return;
        }
        log.debug("开始结算 - 赢家数量: {}, 底注: {}", winners.size(), baseBet);

        // 1. 找出赢家 (手牌为0)
        List<PlayerSeatInfo> losers = new ArrayList<>();

        for (PlayerSeatInfo seat : gameDataVo.getPlayerSeatInfoList()) {
            if (!winners.contains(seat)) {
                losers.add(seat);
            }
        }
        log.debug("结算玩家分布 - 赢家: {}, 输家: {}", winners.stream().map(PlayerSeatInfo::getPlayerId).collect(Collectors.toList()), losers.stream().map(PlayerSeatInfo::getPlayerId).collect(Collectors.toList()));

        long totalWinScore = 0;
        Map<Integer, PokerCard> cardMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));

        // 2. 计算每个输家的输分
        for (PlayerSeatInfo loser : losers) {
            List<Card> handCards = loser.getCurrentCards().stream().map(cardMap::get).collect(Collectors.toList());
            int cardCount = handCards.size();
            handCards.sort(ToSouthHandUtils.CARD_COMPARATOR);
            log.debug("计算输家 {} 分数 - 剩余手牌: {}", loser.getPlayerId(), ToSouthHandUtils.cardListToString(handCards));

            int totalMulti;
            if (context.isInstantWin()) {
                // 通杀：只算张数，一张没出翻倍（13 * 2 = 26），不计算炸弹/红2/黑2
                totalMulti = cardCount * 2;
                log.debug("被通杀的输家 {} - 张数: {}, 翻倍后总倍数: {}", loser.getPlayerId(), cardCount, totalMulti);

            } else {
                // 正常结算：张数 + 红2/黑2 + 最优炸弹倍数（四条与连对共用牌时取最大方案）
                int countTwo = ToSouthHandUtils.countTwo(handCards);
                int countRedTwo = ToSouthHandUtils.countRedTwo(handCards);
                int countBlackTwo = countTwo - countRedTwo;

                // 张数基础倍数：13张（一张没出）翻倍，否则不翻倍
                int cardMulti = (cardCount == 13) ? cardCount * 2 : cardCount;

                // 红2/黑2 倍数（读配置，独立计算）
                int redTwoMulti = moneyCfg.getRemainred2();
                int blackTwoMulti = moneyCfg.getRemainblack2();

                // 炸弹倍数：四条和连对共用牌时，选总倍数最大的方案
                int fourKindBombMulti = moneyCfg.getFourkindboom1();
                int threePairBombMulti = moneyCfg.getRemainBoom1();
                int fourPairBombMulti = moneyCfg.getFourpairsboom1();
                int optimalBombMulti = ToSouthHandUtils.calcOptimalBombMultiplier(
                        handCards, fourKindBombMulti, threePairBombMulti, fourPairBombMulti);

                totalMulti = cardMulti
                        + countRedTwo * redTwoMulti
                        + countBlackTwo * blackTwoMulti
                        + optimalBombMulti;

                log.debug("输家 {} - 张数: {}({}倍), 红2: {}x{}, 黑2: {}x{}, 最优炸弹倍数: {}, 总倍数: {}",
                        loser.getPlayerId(), cardCount, cardMulti,
                        countRedTwo, redTwoMulti, countBlackTwo, blackTwoMulti,
                        optimalBombMulti, totalMulti);
            }
            long loseScore = (long) totalMulti * baseBet;
            // 记录输分 (负数)
            settlementMap.put(loser.getPlayerId(), -loseScore * winners.size());
            totalWinScore += loseScore;
        }
        log.debug("结算总输分: {}, 分配给赢家每人: {}", totalWinScore, totalWinScore);

        // 4. 赢家获得总分
        for (PlayerSeatInfo winner : winners) {
            settlementMap.put(winner.getPlayerId(), totalWinScore);
        }
    }
}
