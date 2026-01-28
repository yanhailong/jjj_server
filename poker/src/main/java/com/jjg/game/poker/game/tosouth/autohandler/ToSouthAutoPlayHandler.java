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

import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.RANK_3;
import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.SPADE_SUITS;

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
            
            // 判断是否必须出黑桃3
            if (gameDataVo.isFirstRound()) {
                // 必须包含黑桃3
                // 找到手牌中的黑桃3
                Card spade3 = handCards.stream()
                        .filter(c -> c.getRank() == RANK_3 && c.getSuit() == SPADE_SUITS)
                        .findFirst()
                        .orElse(null);
                        
                if (spade3 != null) {
                     bestCards = ToSouthHandUtils.findBestPlayWithFirstCard(rankMap, spade3);
                } else {
                    log.error("玩家 {} 是首出玩家且是首轮，但手牌中没有黑桃3", currentPlayerSeat.getPlayerId());
                    // 容错：普通出牌  先打出去再说
                    bestCards = ToSouthHandUtils.findBestPlay(handCards);
                }
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
            reqTurnAction.cards = ToSouthDataHelper.getClientId(gameDataVo, bestCards.stream().map(c -> ((PokerCard)c).getPokerPoolId()).collect(Collectors.toList()));
            reqTurnAction.actionType = 0; // Play
        } else {
            reqTurnAction.actionType = 1; // Pass
        }
        
        log.info("玩家/机器人 {} 是否为首发人员 {} 自动操作: type={}, cards={}", getPlayerId(), isLeader, reqTurnAction.actionType, reqTurnAction.cards);
        assert controller != null;
        controller.turnAction(getPlayerId(), reqTurnAction);
    }
}
