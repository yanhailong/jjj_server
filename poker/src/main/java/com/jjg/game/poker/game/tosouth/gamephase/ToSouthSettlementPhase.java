package com.jjg.game.poker.game.tosouth.gamephase;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.gamephase.BaseSettlementPhase;
import com.jjg.game.poker.game.tosouth.data.ToSouthDataHelper;
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
    private final List<PlayerSeatInfo> winners;

    public ToSouthSettlementPhase(AbstractPhaseGameController<Room_ChessCfg, ToSouthGameDataVo> gameController, List<PlayerSeatInfo> winners) {
        super(gameController);
        this.winners = winners;
    }

    @Override
    public void phaseDoAction() {
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

            // 合并炸弹结算积分
            Map<Long, Long> bombMap = gameDataVo.getBombSettlementMap();
            if (CollUtil.isNotEmpty(bombMap)) {
                for (Map.Entry<Long, Long> entry : bombMap.entrySet()) {
                    settlementMap.merge(entry.getKey(), entry.getValue(), Long::sum);
                }
                log.info("合并炸弹结算积分: {}", bombMap);
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
                    long afterTaxWin = BigDecimal.valueOf(change)
                            .multiply(BigDecimal.valueOf(10000 - gameDataVo.getRoomCfg().getEffectiveRatio()))
                            .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN).longValue();

                    totalTax += (change - afterTaxWin);
                    finalWinScore = afterTaxWin;

                    controller.addItem(playerId, afterTaxWin, AddType.GAME_SETTLEMENT);
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
        // 1. 找出赢家 (手牌为0)
        List<PlayerSeatInfo> losers = new ArrayList<>();

        for (PlayerSeatInfo seat : gameDataVo.getPlayerSeatInfoList()) {
            if (!winners.contains(seat)) {
                losers.add(seat);
            }
        }

        long totalWinScore = 0;
        Map<Integer, PokerCard> cardMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));

        // 2. 计算每个输家的输分
        for (PlayerSeatInfo loser : losers) {
            List<Card> handCards = loser.getCurrentCards().stream().map(cardMap::get).collect(Collectors.toList());
            int cardCount = handCards.size();
            long loseScore = 0;

            // 规则 2a: 剩13张翻倍
            int doubleMulti = (cardCount == 13) ? 2 : 1;
            int countTwo = ToSouthHandUtils.countTwo(handCards);
            int countBomb = ToSouthHandUtils.countBomb(handCards);
            int otherCards = cardCount - countTwo - (countBomb * 4);

            int twoMulti = 0;
            int bombMulti = 0;

            if (countTwo > 0) {
                twoMulti = ToSouthDataHelper.getMultipleByWeight(moneyCfg.getRemain2());
            }
            if (countBomb > 0) {
                bombMulti = ToSouthDataHelper.getMultipleByWeight(moneyCfg.getRemainBoom());
            }
            int totalMulti = doubleMulti * otherCards + countTwo * twoMulti + countBomb * bombMulti;
            loseScore = (long)totalMulti * baseBet;
            // 记录输分 (负数)
            settlementMap.put(loser.getPlayerId(), -loseScore * winners.size());
            totalWinScore += loseScore;
        }
        // 4. 赢家获得总分
        for (PlayerSeatInfo winner : winners) {
            settlementMap.put(winner.getPlayerId(), totalWinScore);
        }
    }
}
