package com.jjg.game.poker.game.douxian.gamephase;

import com.jjg.game.common.concurrent.BaseHandler;
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
 * 已处于托管状态的玩家和机器人一样通过定时任务延迟摆牌；定时回调进入房间线程后，
 * 可以安全检查是否所有活跃玩家都已确认并提前结束阶段。
 * <p>
 * 托管状态跨回合持续：一旦某回合出牌超时进了托管，会一直保持托管状态直到玩家主动取消
 * ({@link DouXianGameController#reqCancelHosting})，不会因为进入新回合而自动清除，
 * 所以每回合出牌阶段开始时都要对"已经在托管中"的玩家做一次自动摆牌+确认。
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
        gameDataVo.getHostingCancelledPlayerIdsThisPhase().clear();
        log.info("斗仙牌进入出牌阶段 round:{} 托管中玩家:{} 机器人和托管玩家会延迟自动摆牌，30s后未确认的会被强制托管",
                gameDataVo.getRound(), gameDataVo.getHostingPlayerIds());
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
        long playerId = gamePlayer.getId();
        if (gameDataVo.getConcededPlayerIds().contains(playerId)) {
            return;
        }
        if (gameController instanceof DouXianGameController controller) {
            int delay = getHostingExecutionDelay();
            log.info("斗仙牌托管玩家{}将在{}ms后自动摆牌 round:{}", playerId, delay, gameDataVo.getRound());
            RobotScheduleUtil.schedule(controller.getRoomController(), new BaseHandler<String>() {
                @Override
                public void action() {
                    if (controller.getCurrentGamePhase() != EGamePhase.PLAY_CART
                            || !gameDataVo.getHostingPlayerIds().contains(playerId)
                            || gameDataVo.getHostingCancelledPlayerIdsThisPhase().contains(playerId)
                            || gameDataVo.getConcededPlayerIds().contains(playerId)
                            || !gameDataVo.getActivePlayerIds().contains(playerId)
                            || gameDataVo.getConfirmedPlayerIds().contains(playerId)) {
                        return;
                    }
                    controller.robotAutoFillAndConfirm(playerId);
                }
            }, delay);
        }
    }

    /**
     * 托管玩家没有机器人 actionId，优先复用本房间机器人的延迟配置；
     * 房间内没有可用机器人配置时，使用 3000ms 兜底值。
     */
    private int getHostingExecutionDelay() {
        for (GamePlayer player : gameDataVo.getGamePlayerMap().values()) {
            if (player instanceof GameRobotPlayer robotPlayer && robotPlayer.getActionId() > 0) {
                return RobotScheduleUtil.getChessExecutionDelay(robotPlayer.getActionId());
            }
        }
        return 3_000;
    }
}
