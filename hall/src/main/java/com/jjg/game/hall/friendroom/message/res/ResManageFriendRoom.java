package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.struct.FriendRoomBaseData;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_CHANGE_FRIEND_ROOM_NAME,
    resp = true
)
@ProtoDesc("响应更新好友房数据")
public class ResManageFriendRoom extends AbstractResponse {

    @ProtoDesc("房间基础信息")
    public FriendRoomBaseData roomBaseData;

    public ResManageFriendRoom(int code) {
        super(code);
    }
}
