package com.jjg.game.poker.game.tosouthfree.autohandler;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BasePokerProcessorHandler;
import com.jjg.game.poker.game.tosouthfree.data.ToSouthFreeDataHelper;
//import com.jjg.game.poker.game.tosouthfree.message.req.ReqTurnAction;
import com.jjg.game.poker.game.tosouthfree.message.req.ReqToSouthFreeTurnAction;
import com.jjg.game.poker.game.tosouthfree.room.ToSouthFreeGameController;
import com.jjg.game.poker.game.tosouthfree.room.data.ToSouthFreeGameDataVo;
import com.jjg.game.poker.game.tosouthfree.util.ToSouthFreeHandUtils;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 南方前进-免费统一自动操作处理器
 * <p>
 * 机器人出牌 → 委托给 {@link ToSouthFreeRobotStrategy}（策略决策）
 * 真实玩家超时 → 首出出最小牌，跟牌直接过牌
 */
public class ToSouthFreeAutoPlayHandler extends BasePokerProcessorHandler<ToSouthFreeGameDataVo> {
    private static final Logger log = LoggerFactory.getLogger(ToSouthFreeAutoPlayHandler.class);
    private final ToSouthFreeGameController controller;

    public ToSouthFreeAutoPlayHandler(long playerId, long gameId, BasePokerGameController<ToSouthFreeGameDataVo> gameController) {
        super(playerId, gameId, gameController);
        if (gameController instanceof ToSouthFreeGameController c) {
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
        ToSouthFreeGameDataVo gameDataVo = controller.getGameDataVo();
        if (gameDataVo.getId() != getId()) return;

        PlayerSeatInfo currentPlayerSeat = gameDataVo.getCurrentPlayerSeatInfo();
        if (currentPlayerSeat == null || currentPlayerSeat.getPlayerId() != getPlayerId()) {
            return;
        }

        doAutoPlay(gameDataVo, currentPlayerSeat);
    }

    private void doAutoPlay(ToSouthFreeGameDataVo gameDataVo, PlayerSeatInfo currentPlayerSeat) {
        ReqToSouthFreeTurnAction reqTurnAction = new ReqToSouthFreeTurnAction();
        reqTurnAction.cards = new ArrayList<>();

        Map<Integer, PokerCard> cardMap = ToSouthFreeDataHelper.getCardListMap(ToSouthFreeDataHelper.getPoolId(gameDataVo));
        List<Card> handCards = currentPlayerSeat.getCurrentCards().stream()
                .map(cardMap::get)
                .collect(Collectors.toList());

        boolean isLeader = gameDataVo.getRoundLeaderSeatId() == currentPlayerSeat.getSeatId()
                && gameDataVo.getLastPlayCards() == null;

        GamePlayer gamePlayer = gameDataVo.getGamePlayer(currentPlayerSeat.getPlayerId());
        boolean isRobot = gamePlayer instanceof GameRobotPlayer;

        List<Card> bestCards = null;

        if (isLeader) {
            if (isRobot) {
                // 机器人：使用策略类决策
                bestCards = ToSouthFreeRobotStrategy.chooseLeaderPlay(handCards, gameDataVo, currentPlayerSeat);
            } else {
                // 真人超时：出最小牌
                bestCards = ToSouthFreeHandUtils.findBestPlay(handCards);
            }
        } else if (!gameDataVo.getCurRoundPassedPlayerSeats().contains(currentPlayerSeat.getSeatId())) {
            if (isRobot) {
                // 机器人：使用策略类决策跟牌
                List<Integer> lastIds = gameDataVo.getLastPlayCards();
                if (CollUtil.isNotEmpty(lastIds)) {
                    List<Card> lastCards = lastIds.stream().map(cardMap::get).collect(Collectors.toList());
                    bestCards = ToSouthFreeRobotStrategy.chooseFollowPlay(handCards, lastCards, gameDataVo, currentPlayerSeat);
                }
            }
            // 真人超时：bestCards=null → 过牌
        }

        if (CollUtil.isNotEmpty(bestCards)) {
            reqTurnAction.cards = ToSouthFreeDataHelper.getClientId(gameDataVo,
                    bestCards.stream().map(c -> ((PokerCard) c).getPokerPoolId()).collect(Collectors.toList()));
            reqTurnAction.actionType = 0; // Play
        } else {
            reqTurnAction.actionType = 1; // Pass
        }

        log.info("玩家/机器人 {} 首轮 {} 首出 {} 自动操作: type={}, cards={}",
                getPlayerId(), gameDataVo.isFirstRound(), isLeader,
                reqTurnAction.actionType, reqTurnAction.cards);
        assert controller != null;
        controller.turnAction(getPlayerId(), reqTurnAction);
    }
}
