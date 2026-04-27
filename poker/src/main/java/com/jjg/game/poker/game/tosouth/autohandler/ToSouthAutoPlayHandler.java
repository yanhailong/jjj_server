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
 * <p>
 * 机器人出牌 → 委托给 {@link ToSouthRobotStrategy}（策略决策）
 * 真实玩家超时 → 首出出最小牌，跟牌直接过牌
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
        if (gameDataVo.getId() != getId()) return;

        PlayerSeatInfo currentPlayerSeat = gameDataVo.getCurrentPlayerSeatInfo();
        if (currentPlayerSeat == null || currentPlayerSeat.getPlayerId() != getPlayerId()) {
            return;
        }

        doAutoPlay(gameDataVo, currentPlayerSeat);
    }

    private void doAutoPlay(ToSouthGameDataVo gameDataVo, PlayerSeatInfo currentPlayerSeat) {
        ReqTurnAction reqTurnAction = new ReqTurnAction();
        reqTurnAction.cards = new ArrayList<>();

        Map<Integer, PokerCard> cardMap = ToSouthDataHelper.getCardListMap(ToSouthDataHelper.getPoolId(gameDataVo));
        List<Card> handCards = currentPlayerSeat.getCurrentCards().stream()
                .map(cardMap::get)
                .collect(Collectors.toList());

        boolean isLeader = gameDataVo.getRoundLeaderSeatId() == currentPlayerSeat.getSeatId()
                && gameDataVo.getLastPlayCards() == null;

        GamePlayer gamePlayer = gameDataVo.getGamePlayer(currentPlayerSeat.getPlayerId());
        boolean isRobot = gamePlayer instanceof GameRobotPlayer;

        List<Card> bestCards = null;

        if (isLeader) {
            // 首出（不能pass）：机器人和真人超时都走机器人出牌逻辑
            // - 新牌局(isFirstRound=true)：必须出黑桃3
            // - 上把赢家首出：出推荐牌
            // - 中途被迫首出（其余人全pass后轮回）：出最佳牌
            bestCards = ToSouthRobotStrategy.chooseLeaderPlay(handCards, gameDataVo, currentPlayerSeat);
        } else if (!gameDataVo.getCurRoundPassedPlayerSeats().contains(currentPlayerSeat.getSeatId())) {
            if (isRobot) {
                // 机器人：使用策略类决策跟牌
                List<Integer> lastIds = gameDataVo.getLastPlayCards();
                if (CollUtil.isNotEmpty(lastIds)) {
                    List<Card> lastCards = lastIds.stream().map(cardMap::get).collect(Collectors.toList());
                    bestCards = ToSouthRobotStrategy.chooseFollowPlay(handCards, lastCards, gameDataVo, currentPlayerSeat);
                }
            }
            // 真人超时可以pass：bestCards=null → 过牌
        }

        if (CollUtil.isNotEmpty(bestCards)) {
            reqTurnAction.cards = ToSouthDataHelper.getClientId(gameDataVo,
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
