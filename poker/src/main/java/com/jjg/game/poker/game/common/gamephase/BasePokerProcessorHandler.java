package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.room.constant.EGamePhase;

import java.util.Objects;

/**
 * @author lm
 * @date 2025/8/8 15:56
 */
public abstract class BasePokerProcessorHandler<T extends BasePokerGameDataVo> implements IProcessorHandler {
    private final long playerId;
    private final long id;
    private final BasePokerGameController<T> gameController;

    public BasePokerProcessorHandler(long playerId, long id, BasePokerGameController<T> gameController) {
        this.gameController = gameController;
        this.playerId = playerId;
        this.id = id;
    }

    @Override
    public void action() throws Exception {
        BasePokerGameDataVo gameDataVo = gameController.getGameDataVo();
        if (gameDataVo.getId() != id) {
            return;
        }
        PlayerSeatInfo currentPlayerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(currentPlayerSeatInfo)) {
            //当前阶段为出牌尝试寻找下一个
            if (getGameController().getCurrentGamePhase() == EGamePhase.PLAY_CART) {
                PlayerSeatInfo nextExePlayer = getGameController().getNextExePlayer();
                if (Objects.nonNull(nextExePlayer)) {
                    addNextPlayer(nextExePlayer);
                } else {
                    getGameController().startNextRoundOrSettlement();
                }
            }
            return;
        }
        if (currentPlayerSeatInfo.getPlayerId() != playerId) {
            return;
        }
        dealAction();
    }

    public abstract void addNextPlayer(PlayerSeatInfo nextPlayerSeatInfo);

    public abstract void dealAction();

    public long getPlayerId() {
        return playerId;
    }

    public long getId() {
        return id;
    }

    public BasePokerGameController<T> getGameController() {
        return gameController;
    }
}
