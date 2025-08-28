package com.jjg.game.poker.game.common.gamephase;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.manager.AbstractRoomManager;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.Map;
import java.util.Objects;


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
        return PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.SETTLEMENT);
    }

    @Override
    public void phaseFinish() {
        if (gameController instanceof BasePokerGameController<T> controller) {
            try {
                //踢未在线的玩家
                Map<Long, RoomPlayer> roomPlayers = controller.getRoom().getRoomPlayers();
                if (CollectionUtil.isNotEmpty(roomPlayers)) {
                    AbstractRoomManager roomManager = controller.getRoomController().getRoomManager();
                    Map<Long, PlayerController> playerControllers = controller.getRoomController().getPlayerControllers();
                    for (RoomPlayer roomPlayer : roomPlayers.values()) {
                        if (!roomPlayer.isOnline()) {
                            PlayerController playerController = playerControllers.get(roomPlayer.getPlayerId());
                            if (Objects.nonNull(playerController)) {
                                roomManager.exitRoom(playerController);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("结算踢不在线人异常", e);
            }
            try {
                phaseFinishDoAction();
            } catch (Exception e) {
                log.error("结算结算处理异常", e);
            }

            controller.setCurrentGamePhase(new BaseWaitReadyPhase<>(gameController));
            gameDataVo.resetData(controller);
            //开启下一局
            controller.tryStartNextGame();
        }
    }

    public void phaseFinishDoAction() {
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.GAME_ROUND_OVER_SETTLEMENT;
    }
}
