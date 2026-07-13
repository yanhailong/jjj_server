package com.jjg.game.poker.game.douxian.gamephase;

import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BasePokerPhase;
import com.jjg.game.poker.game.douxian.autohandler.DouXianRobotHandler;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.room.DouXianGameController;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.robot.RobotScheduleUtil;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

/**
 * 出牌阶段，DESIGN.md 3.3.1/8.6：所有玩家同时把手牌摆入已开放区域，和轮流出牌的
 * {@code BasePlayCardPhase} 不同，这里没有"下一个执行人"的概念，摆牌/确认的校验逻辑
 * 都在 {@link DouXianGameController#reqPlaceCard} / {@link DouXianGameController#reqConfirmPlay} 里。
 * <p>
 * 注意：已经处于托管状态的玩家在 {@link #phaseDoAction()} 里会被自动摆牌+确认，但这里
 * 不会再去检查"是否所有人都确认从而提前结束阶段"——那个检查必须只在 reqConfirmPlay 的调用栈里做，
 * 因为 phaseDoAction 是在 BasePokerGameController#addPokerPhaseTimer 内部同步调用的，
 * 如果在这里提前触发阶段切换会和外层还没执行完的 addPokerPhaseTimer 产生重入冲突。
 */
public class DouXianPlayCardPhase extends BasePokerPhase<DouXianGameDataVo> {

    public DouXianPlayCardPhase(AbstractPhaseGameController<Room_ChessCfg, DouXianGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.PLAY_CART;
    }

    @Override
    public int getPhaseRunTime() {
        return DouXianConstant.Time.PLAY_CARD_TIME;
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        gameDataVo.getConfirmedPlayerIds().clear();
        log.info("斗仙牌进入出牌阶段 round:{} 托管中玩家:{} 机器人会按各自延迟自动摆牌，30s后未确认的会被强制托管",
                gameDataVo.getRound(), gameDataVo.getHostingPlayerIds());
        if (gameController instanceof DouXianGameController controller) {
            for (PlayerSeatInfo seatInfo : gameDataVo.getPlayerSeatInfoList()) {
                if (seatInfo.isDelState()) {
                    continue;
                }
                long playerId = seatInfo.getPlayerId();
                if (gameDataVo.getConcededPlayerIds().contains(playerId)) {
                    continue;
                }
                if (gameDataVo.getHostingPlayerIds().contains(playerId)) {
                    controller.autoFillAndConfirm(playerId);
                }
            }
        }
    }

    @Override
    public void phaseFinish() {
        if (gameController instanceof DouXianGameController controller) {
            controller.forceFinishPlayCardPhase();
        }
    }

    @Override
    protected void robotActionOnPhaseStart(GameRobotPlayer gamePlayer) {
        if (gameDataVo.getConcededPlayerIds().contains(gamePlayer.getId())) {
            return;
        }
        if (gameController instanceof DouXianGameController controller) {
            int delay = RobotScheduleUtil.getChessExecutionDelay(gamePlayer.getActionId());
            log.info("斗仙牌机器人{}将在{}ms后摆牌 round:{}", gamePlayer.getId(), delay, gameDataVo.getRound());
            DouXianRobotHandler handler = new DouXianRobotHandler(gamePlayer, DouXianRobotHandler.PLAY_CARD, controller);
            RobotScheduleUtil.schedule(controller.getRoomController(), handler, delay);
        }
    }

    @Override
    protected void hostingPlayerActionOnPhaseStart(GamePlayer gamePlayer) {
    }
}
