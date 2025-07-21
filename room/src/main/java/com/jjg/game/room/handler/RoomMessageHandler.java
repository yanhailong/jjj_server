package com.jjg.game.room.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.listener.RoomEventListener;
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
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class RoomMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private RoomEventListener playerEventListener;

    @Command(MessageConst.ToServer.REQ_EXIT_GAME)
    public void reqExitGame(PlayerController playerController, ReqExitGame req) {
        try{
            log.debug("退出游戏 playerId = {}",playerController.playerId());
            playerEventListener.exitGame(playerController);

            playerController.send(new ResExitGame(Code.SUCCESS));
        }catch (Exception e) {
            log.error("", e);
        }
    }
}
