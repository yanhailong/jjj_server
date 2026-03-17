package com.jjg.game.poker.game.tosouth.gamephase;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.Card;
import com.jjg.game.core.data.RoomPlayer;
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
import java.util.*;
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

            calSettlement(gameDataVo,settlementMap, baseBet, moneyCfg);

            // 记录本局赢家，供下局判断首出玩家
            if (!winners.isEmpty()) {
                gameDataVo.setLastGameWinnerPlayerId(winners.getFirst().getPlayerId());
            }

            // 应用结算结果
            long totalTax = 0;
            List<ToSouthPlayerSettlementInfo> playerSettlementInfos = new ArrayList<>();
            for (Map.Entry<Long, Long> entry : settlementMap.entrySet()) {
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
                    }
                } else {
                    long loseAmount = -change;
                    if (loseAmount > 0) {
                        controller.deductItem(playerId, loseAmount, AddType.GAME_SETTLEMENT, "南方前进输钱", false);
                    }

                    if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
                        robotPlayer.setLastWin(2);
                    } else {
                        controller.dealLose(gamePlayer, change);
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
            log.info("南方前进结算map: {}", settlementMap);
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
            long loseScore = 0;

            // 统计手牌中的特殊牌型
            int countTwo = ToSouthHandUtils.countTwo(handCards);
            int countRedTwo = ToSouthHandUtils.countRedTwo(handCards);
            int countBlackTwo = countTwo - countRedTwo;
            int countBomb = ToSouthHandUtils.countBomb(handCards);
            int countConsecPairBomb = ToSouthHandUtils.countConsecutivePairBombs(handCards);

            int redTwoMulti = countRedTwo > 0 ? moneyCfg.getRemainred2() : 0;
            int blackTwoMulti = countBlackTwo > 0 ? moneyCfg.getRemainblack2() : 0;
            int fourKindBombMulti = countBomb > 0 ? moneyCfg.getFourkindboom() : 0;
            int fourPairsBombMulti = countConsecPairBomb > 0 ? moneyCfg.getFourpairsboom() : 0;

            int totalMulti = 0;
            if (context.isInstantWin()) {
                // 通杀：不翻倍(doubleMulti=1)，但仍计算特殊牌型赔率
                totalMulti = cardCount
                        + countRedTwo * redTwoMulti
                        + countBlackTwo * blackTwoMulti
                        + countBomb * fourKindBombMulti
                        + countConsecPairBomb * fourPairsBombMulti;
                log.debug("被通杀的输家 {} - 红2: {}x{}, 黑2: {}x{}, 四条: {}x{}, 连对炸: {}x{}, 总倍数: {}",
                        loser.getPlayerId(), countRedTwo, redTwoMulti, countBlackTwo, blackTwoMulti,
                        countBomb, fourKindBombMulti, countConsecPairBomb, fourPairsBombMulti, totalMulti);

            } else {
                int doubleMulti = (cardCount == 13) ? 2 : 1;
                log.debug("输家 {} 手牌构成 - 13张翻倍: {}, 红2数量: {}, 黑2数量: {}, 四条炸弹数量: {}, 连对炸弹数量: {}",
                        loser.getPlayerId(), doubleMulti, countRedTwo, countBlackTwo, countBomb, countConsecPairBomb);

                totalMulti = doubleMulti * cardCount
                        + countRedTwo * redTwoMulti
                        + countBlackTwo * blackTwoMulti
                        + countBomb * fourKindBombMulti
                        + countConsecPairBomb * fourPairsBombMulti;
                log.debug("输家 {} 计算结果 - 红2倍数: {}, 黑2倍数: {}, 四条炸弹倍数: {}, 连对炸弹倍数: {}, 总倍数: {}",
                        loser.getPlayerId(), redTwoMulti, blackTwoMulti, fourKindBombMulti, fourPairsBombMulti, totalMulti);

            }
            loseScore = (long)totalMulti * baseBet;
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
