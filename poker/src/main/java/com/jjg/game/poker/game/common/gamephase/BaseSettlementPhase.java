package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

/**
 * 通用扑克游戏结算阶段
 *
 * @author lm
 * @date 2025/7/26 11:06
 */
public abstract class BaseSettlementPhase<T extends BasePokerGameDataVo> extends BasePokerPhase<T> {
    public BaseSettlementPhase(AbstractPhaseGameController<Room_ChessCfg, T> gameController) {
        super(gameController);
    }


    @Override
    public int getPhaseRunTime() {
        return PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.SETTLEMENT);
    }

    @Override
    public void phaseFinish() {
        if (gameController instanceof BasePokerGameController<T> controller) {
            phaseFinishDoAction();
            controller.setCurrentGamePhase(new BaseWaitReadyPhase<>(gameController));
            gameDataVo.resetData(controller);
            //开启下一局
            controller.tryStartGame();
        }
    }

    public void phaseFinishDoAction() {
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.GAME_ROUND_OVER_SETTLEMENT;
    }
}
