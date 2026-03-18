package com.jjg.game.poker.game.tosouth.autohandler;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.poker.game.tosouth.room.ToSouthGameController;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 南方前进准备超时处理器
 * 玩家在 WAIT_READY 阶段 10 秒内未点击准备，自动踢出房间
 */
public class ToSouthReadyTimeoutHandler implements IProcessorHandler {
    private static final Logger log = LoggerFactory.getLogger(ToSouthReadyTimeoutHandler.class);

    private final long playerId;
    private final long gameId;
    private final ToSouthGameController controller;

    public ToSouthReadyTimeoutHandler(long playerId, long gameId, ToSouthGameController controller) {
        this.playerId = playerId;
        this.gameId = gameId;
        this.controller = controller;
    }

    @Override
    public void action() {
        ToSouthGameDataVo gameDataVo = controller.getGameDataVo();
        // 验证游戏ID一致（防止跨局触发）
        if (gameDataVo.getId() != gameId) {
            return;
        }
        // 阶段已切换（已开局），不再处理
        if (controller.getCurrentGamePhase() != EGamePhase.WAIT_READY) {
            return;
        }
        // 玩家已准备，不处理
        if (gameDataVo.getReadyPlayerIds().contains(playerId)) {
            return;
        }
        // 玩家超时未准备，踢出房间
        log.info("玩家 {} 准备超时(10秒)，踢出房间", playerId);
        controller.kickUnreadyPlayer(playerId);
    }
}
