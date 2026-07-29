package com.jjg.game.poker.game.douxian.gamephase;

import com.jjg.game.poker.game.common.gamephase.BasePokerPhase;
import com.jjg.game.poker.game.douxian.autohandler.DouXianRobotHandler;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.message.resp.NotifyDouXianDiscardStart;
import com.jjg.game.poker.game.douxian.room.DouXianGameController;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.robot.RobotScheduleUtil;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

/**
 * 弃牌阶段，DESIGN.md 3.3.4/8.11：玩家可以弃置任意手牌或选择不弃，补牌统一放到
 * 下一回合(或本局结束)的 {@link DouXianDealPhase} 里做。
 */
public class DouXianDiscardPhase extends BasePokerPhase<DouXianGameDataVo> {

    public DouXianDiscardPhase(AbstractPhaseGameController<Room_ChessCfg, DouXianGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.DISCARD;
    }

    @Override
    public int getPhaseRunTime() {
        return DouXianConstant.Time.DISCARD_TIME;
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        gameDataVo.getDiscardedPlayerIds().clear();
        NotifyDouXianDiscardStart notify = new NotifyDouXianDiscardStart();
        notify.currentRound = gameDataVo.getRound();
        notify.nextRound = gameDataVo.getRound() < DouXianConstant.Common.TOTAL_ROUND
                ? gameDataVo.getRound() + 1 : 0;
        notify.overTime = gameDataVo.getPhaseEndTime();
        broadcastMsgToRoom(notify);
        log.info("斗仙牌进入弃牌阶段 round:{}", gameDataVo.getRound());
        if (gameController instanceof DouXianGameController controller) {
            for (Long playerId : gameDataVo.getActivePlayerIds()) {
                if (gameDataVo.getHostingPlayerIds().contains(playerId)) {
                    controller.autoNoDiscard(playerId);
                }
            }
        }
    }

    @Override
    public void phaseFinish() {
        if (gameController instanceof DouXianGameController controller) {
            controller.forceFinishDiscardPhase();
        }
    }

    @Override
    protected void robotActionOnPhaseStart(GameRobotPlayer gamePlayer) {
        if (gameDataVo.getConcededPlayerIds().contains(gamePlayer.getId())) {
            return;
        }
        if (gameController instanceof DouXianGameController controller) {
            int delay = RobotScheduleUtil.getChessExecutionDelay(gamePlayer.getActionId());
            log.info("斗仙牌机器人{}将在{}ms后弃牌 round:{}", gamePlayer.getId(), delay, gameDataVo.getRound());
            DouXianRobotHandler handler = new DouXianRobotHandler(gamePlayer, DouXianRobotHandler.DISCARD, controller);
            RobotScheduleUtil.schedule(controller.getRoomController(), handler, delay);
        }
    }

    @Override
    protected void hostingPlayerActionOnPhaseStart(GamePlayer gamePlayer) {
    }
}
