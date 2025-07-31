package com.jjg.game.slots.handler;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.ReqExitGame;
import com.jjg.game.core.pb.ResExitGame;
import com.jjg.game.slots.manager.SlotsPlayerEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/7/25 15:27
 */
@Component
@MessageType(MessageConst.MessageTypeDef.ROOM_TYPE)
public class SlotsRoomMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private SlotsPlayerEventListener slotsPlayerEventListener;

    @Command(MessageConst.RoomMessage.REQ_EXIT_GAME)
    public void reqExitGame(PlayerController playerController, ReqExitGame req) {
        try {
            log.debug("退出游戏 playerId = {}", playerController.playerId());
            slotsPlayerEventListener.exitGame(playerController.getSession());
            clusterSystem.switchNode(playerController.getSession(), NodeType.HALL);
            playerController.send(new ResExitGame(Code.SUCCESS));
        } catch (Exception e) {
            log.error("玩家退出房间异常 msg: {}", e.getMessage(), e);
        }
    }
}
