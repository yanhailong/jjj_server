package com.jjg.game.hall.pb.friendroom.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.friendroom.struct.RoomFriendEnum;

/**
 * 请求操作房间好友列表
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.REQ_OPERATE_ROOM_FRIENDS_LIST
)
@ProtoDesc("请求操作关注好友列表")
public class ReqOperateRoomFriendsList  extends AbstractMessage {

    @ProtoDesc("操作类型")
    public RoomFriendEnum operate;

    @ProtoDesc("操作的玩家ID")
    public long playerId;
}
