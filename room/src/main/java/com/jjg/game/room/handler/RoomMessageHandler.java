package com.jjg.game.room.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.ReqExitGame;
import com.jjg.game.core.pb.ResExitGame;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.listener.RoomEventListener;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author 11
 * @date 2025/7/15 15:23
 */
@Component
@MessageType(MessageConst.MessageTypeDef.ROOM_TYPE)
public class RoomMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private RoomManager roomManager;

    @Autowired
    private RoomEventListener playerEventListener;

    @Command(MessageConst.RoomMessage.REQ_EXIT_GAME)
    public void reqExitGame(PlayerController playerController, ReqExitGame req) {
        try {
            long playerId = playerController.playerId();
            log.debug("退出游戏 playerId = {}", playerId);
            if (playerController.getPlayer().getGameType() != EGameType.BACCARAT.getGameTypeId()) {
                AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                    roomManager.getGameControllerByPlayerId(playerId);
                if (Objects.isNull(gameController)) {
                    playerController.send(new ResExitGame(Code.PARAM_ERROR));
                    return;
                }
                if (!gameController.canExitGame(playerId)) {
                    playerController.send(new ResExitGame(Code.FORBID));
                    return;
                }
            }
            int code = playerEventListener.exitGame(playerController);
            playerController.send(new ResExitGame(code));
        } catch (Exception e) {
            log.error("玩家退出房间异常 msg: {}", e.getMessage(), e);
        }
    }


    /**
     * 请求申请成为庄家
     */
    public void reqApplyBanker(PlayerController playerController) {

    }

    /**
     * 取消成为庄家
     */
    public void reqCancelBeBanker() {

    }

    /**
     * 请求庄家列表
     */
    public void reqBankerList() {

    }
}
