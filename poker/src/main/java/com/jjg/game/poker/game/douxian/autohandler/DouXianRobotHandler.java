package com.jjg.game.poker.game.douxian.autohandler;

import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.gamephase.BasePokerRobotProcessorHandler;
import com.jjg.game.poker.game.douxian.room.DouXianGameController;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 斗仙牌机器人调度。斗仙牌没有 blackjack 那种按点数查权重表的策略配置(没有对应的斗仙牌
 * ChessJackStrategyCfg 类似表)，机器人直接复用玩家超时托管同一套"仙>灵>凡贪心找最优组合"
 * 算法({@link DouXianGameController#autoFillAndConfirm})，只是不用等到阶段超时，
 * 在阶段开始时就按 {@link com.jjg.game.room.robot.RobotScheduleUtil#getChessExecutionDelay}
 * 给的延迟去执行，看起来像是"在思考"而不是卡到最后一刻。
 */
public class DouXianRobotHandler extends BasePokerRobotProcessorHandler<DouXianGameDataVo> {

    private static final Logger log = LoggerFactory.getLogger(DouXianRobotHandler.class);

    //出牌阶段：摆牌+确认
    public static final int PLAY_CARD = 1;
    //弃牌阶段：选择不弃(和托管默认行为一致，DESIGN.md 5.)
    public static final int DISCARD = 2;

    public DouXianRobotHandler(GameRobotPlayer gameRobotPlayer, int type, BasePokerGameController<DouXianGameDataVo> gameController) {
        super(gameRobotPlayer, type, gameController);
    }

    @Override
    public void action() {
        BasePokerGameController<DouXianGameDataVo> gameController = getGameController();
        if (!(gameController instanceof DouXianGameController controller)) {
            return;
        }
        long playerId = getPlayerId();
        DouXianGameDataVo gameDataVo = controller.getGameDataVo();
        if (gameDataVo.getConcededPlayerIds().contains(playerId)) {
            return;
        }
        switch (getType()) {
            case PLAY_CARD -> {
                if (controller.getCurrentGamePhase() != EGamePhase.PLAY_CART) {
                    return;
                }
                controller.autoFillAndConfirm(playerId);
            }
            case DISCARD -> {
                if (controller.getCurrentGamePhase() != EGamePhase.DISCARD) {
                    return;
                }
                controller.autoNoDiscard(playerId);
            }
            default -> log.warn("斗仙牌机器人未知的调度类型 playerId:{} type:{}", playerId, getType());
        }
    }
}
