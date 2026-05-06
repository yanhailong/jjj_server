package com.jjg.game.poker.game.tosouthblood.autohandler;

import cn.hutool.core.util.RandomUtil;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.gamephase.BasePokerRobotProcessorHandler;
import com.jjg.game.poker.game.tosouthblood.room.ToSouthBloodGameController;
import com.jjg.game.poker.game.tosouthblood.room.data.ToSouthBloodGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 南方前进-血战机器人行为处理器
 * <p>
 * 参考 TexasRobotHandler 模式，通过 RobotScheduleUtil.schedule 延迟调度执行。
 * 当前支持 GO_READY 行为：机器人在 WAIT_READY 阶段按概率决定准备或退出。
 *
 * @author Auto
 */
public class ToSouthBloodRobotHandler extends BasePokerRobotProcessorHandler<ToSouthBloodGameDataVo> {

    private static final Logger log = LoggerFactory.getLogger(ToSouthBloodRobotHandler.class);

    /** 行为类型：准备 */
    public static final int GO_READY = 1;

    /** 准备概率（万分比，0-10000） */
    private final int readyPro;

    public ToSouthBloodRobotHandler(GameRobotPlayer gameRobotPlayer, int type,
                                   BasePokerGameController<ToSouthBloodGameDataVo> gameController, int readyPro) {
        super(gameRobotPlayer, type, gameController);
        this.readyPro = readyPro;
    }

    @Override
    public void action() {
        BasePokerGameController<ToSouthBloodGameDataVo> gameController = getGameController();
        if (gameController instanceof ToSouthBloodGameController controller) {
            switch (getType()) {
                case GO_READY -> {
                    if (controller.getCurrentGamePhase() != EGamePhase.WAIT_READY) {
                        log.debug("机器人 {} GO_READY 触发时不在 WAIT_READY 阶段，跳过", getPlayerId());
                        return;
                    }
                    if (readyPro > RandomUtil.randomInt(GameConstant.TEN_THOUSAND)) {
                        // 概率通过 → 机器人准备
                        controller.robotGoReady(getPlayerId());
                    } else {
                        // 概率未通过 → 机器人退出房间
                        log.info("机器人 {} 准备概率未通过 (pro={}), 退出房间", getPlayerId(), readyPro);
                        controller.robotExitRoom(getPlayerId());
                    }
                }
                default -> log.warn("ToSouthBloodRobotHandler 未知行为类型: {}", getType());
            }
        }
    }
}
