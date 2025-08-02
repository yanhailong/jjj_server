package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.message.reps.NotifyPhaseChange;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.Map;

/**
 * 开始游戏阶段
 *
 * @author lm
 * @date 2025/7/26 15:01
 */
public abstract class BaseStartGamePhase<T extends BasePokerGameDataVo> extends BasePokerPhase<T> {
    public BaseStartGamePhase(AbstractGameController<Room_ChessCfg, T> gameController) {
        super(gameController);
    }


    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        NotifyPhaseChange notifyPhaseChange = PokerBuilder.buildNotifyPhaseChange(getGamePhase(), getPhaseRunTime());
        broadcastMsgToRoom(notifyPhaseChange);
    }

    @Override
    public void phaseFinish() {
        //人数足够开始游戏 不够回退到等待阶段
        int total = gameDataVo.getSeatDownNum();
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        if (roomCfg.getMinPlayer() >= total && total <= roomCfg.getMaxPlayer()) {
            //进行下一个阶段
            nextPhase();
            NotifyPhaseChange notifyPhaseChange = PokerBuilder.buildNotifyPhaseChange(gameController.getCurrentGamePhase(), gameDataVo.getPhaseEndTime());
            broadcastMsgToRoom(notifyPhaseChange);
        } else {
            //重新等待
            if (gameController instanceof BasePokerGameController<T> basePokerGameController) {
                basePokerGameController.goBackWaitReadyPhase();
            }
            NotifyPhaseChange notifyPhaseChange = PokerBuilder.buildNotifyPhaseChange(EGamePhase.WAIT_READY, -1);
            broadcastMsgToRoom(notifyPhaseChange);
        }
    }


    @Override
    public int getPhaseRunTime() {
        return TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.PREPARE);
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.START_GAME;
    }
}
