package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.struct.FriendRoomBaseData;

/**
 * 请求更新房间名
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_CHANGE_FRIEND_ROOM_NAME
)
@ProtoDesc("请求更新房间名")
public class ReqUpdateFriendRoom extends AbstractMessage {

    @ProtoDesc("房间ID")
    public long roomId;

    @ProtoDesc("申请开房时长，RoomExpend cfg id")
    public int timeOfOpenRoom;

    @ProtoDesc("是否自动续费")
    public boolean autoRenewal;

    @ProtoDesc("添加的庄家准备金")
    public long predictCostGoldNum;

    @ProtoDesc("房间名")
    public String roomAliasName;
}
