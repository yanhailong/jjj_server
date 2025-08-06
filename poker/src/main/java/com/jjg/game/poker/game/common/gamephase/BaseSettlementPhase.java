package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.Map;

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
        return TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.SETTLEMENT);
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.GAME_ROUND_OVER_SETTLEMENT;
    }
}
