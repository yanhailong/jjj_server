package com.jjg.game.room.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.listener.RoomEventListener;
import com.jjg.game.room.message.RoomMessageConstant;
import com.jjg.game.room.pb.ReqExitGame;
import com.jjg.game.room.pb.ResExitGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/7/15 15:23
 */
@Component
@MessageType(MessageConst.MessageTypeDef.ROOM_TYPE)
public class RoomMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private RoomEventListener playerEventListener;

    @Command(RoomMessageConstant.ReqMsgBean.REQ_EXIT_GAME)
    public void reqExitGame(PlayerController playerController, ReqExitGame req) {
        try {
            log.debug("退出游戏 playerId = {}", playerController.playerId());
            int code = playerEventListener.exitGame(playerController);
            playerController.send(new ResExitGame(code));
        } catch (Exception e) {
            log.error("玩家退出房间异常 msg: {}", e.getMessage(), e);
        }
    }
}
