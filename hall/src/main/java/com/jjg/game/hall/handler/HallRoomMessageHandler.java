package com.jjg.game.hall.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.ReqExitGame;
import com.jjg.game.core.pb.ResExitGame;
import com.jjg.game.ploy.controller.AbstractPloyController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/3/25
 */
@Component
@MessageType(MessageConst.MessageTypeDef.ROOM_TYPE)
public class HallRoomMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Command(MessageConst.RoomMessage.REQ_EXIT_GAME)
    public void reqExitGame(PlayerController playerController, ReqExitGame req) {
        ResExitGame res = new ResExitGame(Code.SUCCESS);
        try {
            if (playerController.getSubScene() instanceof AbstractPloyController<?> ployController) {
                res.code = ployController.exitGame(playerController.getPlayer(), ExitType.INITIATIVE);
                playerController.setSubScene(null);
            }
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("玩家退出房间异常 msg: {}", e.getMessage(), e);
        }
        playerController.send(res);
    }
}
