package com.jjg.game.room.message;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.room.message.resp.RespPlayerExitRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.jjg.game.room.message.RoomMessageConstant.ReqMsgBean.REQ_EXIT_ROOM;

/**
 * 房间内的通用消息处理器
 *
 * @author 2CL
 */
@MessageType(value = MessageConst.MessageTypeDef.ROOM_TYPE)
@ProtoDesc("房间消息处理类")
@Component
public class RoomMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(RoomMessageHandler.class);
    // 房间管理器
    @Autowired
    private RoomManager roomManager;
    @Autowired
    NodeManager nodeManager;
    @Autowired
    ClusterSystem clusterSystem;


    @Command(REQ_EXIT_ROOM)
    public void playerExitRoom(PlayerController playerController) {
        try {
            int code = roomManager.exitRoom(playerController);
            RespPlayerExitRoom respPlayerExitRoom = new RespPlayerExitRoom();
            respPlayerExitRoom.code = code;
            respPlayerExitRoom.playerId = playerController.playerId();
            playerController.send(respPlayerExitRoom);
            // 将玩家切回大厅
            MarsNode marsNode = nodeManager.getMarNode(NodeType.HALL);
            //切换节点
            clusterSystem.switchNode(playerController.getSession(), marsNode);
        } catch (Exception exception) {
            log.error("玩家: {} 退出房间时发生异常: {}", playerController.playerId(), exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }
}
