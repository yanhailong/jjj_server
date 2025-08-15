package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.struct.FriendRoomBaseData;

import java.util.List;

/**
 * 返回好友房列表
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_FRIEND_ROOM_LIST,
    resp = true
)
@ProtoDesc("返回关注的好友的房间列表")
public class ResFriendRoomList extends AbstractResponse {

    @ProtoDesc("好友房间基础信息列表")
    public List<FriendRoomBaseData> roomList;

    public ResFriendRoomList(int code) {
        super(code);
    }
}
