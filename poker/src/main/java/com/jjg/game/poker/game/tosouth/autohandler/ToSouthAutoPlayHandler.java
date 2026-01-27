package com.jjg.game.poker.game.tosouth.autohandler;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BasePokerProcessorHandler;
import com.jjg.game.poker.game.tosouth.data.ToSouthDataHelper;
import com.jjg.game.poker.game.tosouth.message.req.ReqTurnAction;
import com.jjg.game.poker.game.tosouth.room.ToSouthGameController;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthGameDataVo;
import com.jjg.game.poker.game.tosouth.util.ToSouthHandUtils;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 南方前进统一自动操作处理器
 * (适用于机器人和真实玩家超时托管)
 */
public class ToSouthAutoPlayHandler extends BasePokerProcessorHandler<ToSouthGameDataVo> {
    private static final Logger log = LoggerFactory.getLogger(ToSouthAutoPlayHandler.class);
    private final ToSouthGameController controller;

    public ToSouthAutoPlayHandler(long playerId, long gameId, BasePokerGameController<ToSouthGameDataVo> gameController) {
        super(playerId, gameId, gameController);
        if (gameController instanceof ToSouthGameController c) {
            this.controller = c;
        } else {
            this.controller = null;
        }
    }

    @Override
    public void addNextPlayer(PlayerSeatInfo nextPlayerSeatInfo) {
    }

    @Override
    public void dealAction() {
        action();
    }

    @Override
    public void action() {
        if (controller == null) return;
        ToSouthGameDataVo gameDataVo = controller.getGameDataVo();
        // 检查游戏 ID 是否匹配
        if (gameDataVo.getId() != getId()) return;

        PlayerSeatInfo currentPlayerSeat = gameDataVo.getCurrentPlayerSeatInfo();
        if (currentPlayerSeat == null || currentPlayerSeat.getPlayerId() != getPlayerId()) {
            return;
        }

        // 无论是机器人还是玩家超时，都执行相同的自动出牌逻辑
        doAutoPlay(gameDataVo, currentPlayerSeat);
    }

    private void doAutoPlay(ToSouthGameDataVo gameDataVo, PlayerSeatInfo currentPlayerSeat) {
        ReqTurnAction reqTurnAction = new ReqTurnAction();
        reqTurnAction.cards = new ArrayList<>();

        Map<Integer, PokerCard> cardMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));
        List<Card> handCards = currentPlayerSeat.getCurrentCards().stream()
                .map(cardMap::get)
                .collect(Collectors.toList());

        boolean isLeader = gameDataVo.getRoundLeaderSeatId() == currentPlayerSeat.getSeatId() && gameDataVo.getLastPlayCards() == null;
        List<Card> bestCards = null;

        if (isLeader) {
            // 首出
            Map<Integer, List<Card>> rankMap = ToSouthHandUtils.convertCardListToRankMap(handCards);
            // 只有在首局首出且手牌确实包含黑桃3时，才强制走黑桃3 逻辑
            boolean hasSpade3 = handCards.stream().anyMatch(c -> c.getRank() == 3 && c.getSuit() == 0);
            
            if (gameDataVo.isFirstRound() && hasSpade3) {
                // 首局首出 (找含黑桃3的)
                bestCards = handCards.stream()
                        .filter(c -> c.getRank() == 3 && c.getSuit() == 0)
                        .findFirst()
                        .map(spade3 -> ToSouthHandUtils.findBestPlayWithFirstCard(rankMap, spade3))
                        .orElse(null);
            } else {
                bestCards = ToSouthHandUtils.findBestPlay(handCards);
            }
        } else {
            // 跟牌
            // 检查是否为机器人
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(currentPlayerSeat.getPlayerId());
            boolean isRobot = gamePlayer instanceof GameRobotPlayer;

            // 只有机器人会尝试跟牌，真实玩家超时直接过牌
            if (isRobot) {
                List<Integer> lastIds = gameDataVo.getLastPlayCards();
                if (CollUtil.isNotEmpty(lastIds)) {
                    List<Card> lastCards = lastIds.stream().map(cardMap::get).collect(Collectors.toList());
                    bestCards = ToSouthHandUtils.findBestFollowPlay(handCards, lastCards);
                }
            }
        }

        if (CollUtil.isNotEmpty(bestCards)) {
            reqTurnAction.cards = ToSouthDataHelper.getClientId(gameDataVo, bestCards.stream().map(Card::getValue).collect(Collectors.toList()));
            reqTurnAction.actionType = 0; // Play
        } else {
            reqTurnAction.actionType = 1; // Pass
        }
        
        log.info("玩家/机器人 {} 自动操作: type={}, cards={}", getPlayerId(), reqTurnAction.actionType, reqTurnAction.cards);
        assert controller != null;
        controller.turnAction(getPlayerId(), reqTurnAction);
    }
}
