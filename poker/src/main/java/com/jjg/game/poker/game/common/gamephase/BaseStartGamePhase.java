package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerPhaseChange;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_ChessCfg;


/**
 * 开始游戏阶段
 *
 * @author lm
 * @date 2025/7/26 15:01
 */
public abstract class BaseStartGamePhase<T extends BasePokerGameDataVo> extends BasePokerPhase<T> {
    //执行的当局游戏id
    private final long executionGameId;

    public BaseStartGamePhase(AbstractPhaseGameController<Room_ChessCfg, T> gameController, long executionGameId) {
        super(gameController);
        this.executionGameId = executionGameId;
    }


    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        NotifyPokerPhaseChange notifyPokerPhaseChange = PokerBuilder.buildNotifyPhaseChange(getGamePhase(), gameDataVo.getPhaseEndTime());
        broadcastMsgToRoom(notifyPokerPhaseChange);
    }

    @Override
    public void phaseFinish() {
        //人数足够开始游戏 不够回退到等待阶段
        int total = gameDataVo.getSeatDownNum();
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        if (getGamePhase() != EGamePhase.START_GAME || executionGameId != gameDataVo.getId()) {
            return;
        }
        if (roomCfg.getMinPlayer() <= total && total <= roomCfg.getMaxPlayer()) {
            //进行下一个阶段
            nextPhase();
            NotifyPokerPhaseChange notifyPokerPhaseChange = PokerBuilder.buildNotifyPhaseChange(gameController.getCurrentGamePhase(), gameDataVo.getPhaseEndTime());
            broadcastMsgToRoom(notifyPokerPhaseChange);
        } else {
            //重新等待
            if (gameController instanceof BasePokerGameController<T> basePokerGameController) {
                basePokerGameController.goBackWaitReadyPhase();
            }
            onPhaseReset();
            NotifyPokerPhaseChange notifyPokerPhaseChange = PokerBuilder.buildNotifyPhaseChange(EGamePhase.WAIT_READY, -1);
            broadcastMsgToRoom(notifyPokerPhaseChange);
        }
    }

    /**
     * 当阶段重置时调用
     */
    public void onPhaseReset() {

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
