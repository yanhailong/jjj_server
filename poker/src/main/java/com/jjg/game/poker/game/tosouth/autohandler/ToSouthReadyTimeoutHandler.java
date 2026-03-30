package com.jjg.game.poker.game.tosouth.autohandler;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PokerRoom;
import com.jjg.game.core.data.Room;
import com.jjg.game.poker.game.tosouth.room.ToSouthGameController;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
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
    private final long timerVersion;
    private final AbstractRoomController<Room_ChessCfg, ? extends Room> roomController;

    public ToSouthReadyTimeoutHandler(long playerId, long gameId, long timerVersion, AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
        this.playerId = playerId;
        this.gameId = gameId;
        this.timerVersion = timerVersion;
        this.roomController = roomController;
    }

    @Override
    public void action() {
        ToSouthGameController gameController = (ToSouthGameController) roomController.getGameController();
        if (gameController == null) {
            return;
        }
        if (gameController instanceof AbstractPhaseGameController<?, ?> phaseGameController) {
            ToSouthGameDataVo gameDataVo = (ToSouthGameDataVo) roomController.getGameController().getGameDataVo();

            // 验证游戏ID一致（防止跨局触发）
            if (gameDataVo.getId() != gameId) {
                return;
            }
            // 阶段已切换（已开局），不再处理
            if (gameController.getCurrentGamePhase() != EGamePhase.WAIT_READY) {
                return;
            }
            // 验证定时器版本号（玩家退出后重进会递增版本号，使旧定时器失效）
            Long currentVersion = gameDataVo.getReadyTimerVersion().get(playerId);

            log.info("currentVersion:{};timerVersion:{}", currentVersion, timerVersion);
            if (currentVersion == null || currentVersion != timerVersion) {
                log.debug("玩家 {} 准备倒计时版本不匹配（当前:{}, 定时器:{}），跳过", playerId, currentVersion, timerVersion);
                return;
            }
            // 玩家已准备，不处理
            if (gameDataVo.getReadyPlayerIds().contains(playerId)) {
                return;
            }
            // 玩家超时未准备，踢出房间
            log.info("玩家 {} 准备超时(10秒)，踢出房间", playerId);
            gameController.kickUnreadyPlayer(playerId);
        }
    }
}
