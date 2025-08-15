package com.jjg.game.hall.friendroom.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.req.ReqCreateFriendsRoom;
import com.jjg.game.hall.friendroom.services.FriendRoomServices;
import com.jjg.game.hall.room.HallRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 好友房消息handler
 *
 * @author 2CL
 */
@Component
@MessageType(MessageConst.MessageTypeDef.FRIEND_ROOM_TYPE)
public class FriendRoomMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(FriendRoomMessageHandler.class);
    @Autowired
    private FriendRoomServices friendRoomServices;
    @Autowired
    private HallRoomService hallRoomService;

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_CREAT_FRIENDS_ROOM)
    public void reqCreateFriendRoom(PlayerController playerController, ReqCreateFriendsRoom reqCreateFriendsRoom) {
        try {
            friendRoomServices.createFriendRoom(playerController, reqCreateFriendsRoom);
        } catch (Exception e) {
            log.error("创建服务器异常");
        }
    }
}
