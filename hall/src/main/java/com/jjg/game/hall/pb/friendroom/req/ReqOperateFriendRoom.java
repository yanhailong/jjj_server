package com.jjg.game.hall.pb.friendroom.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * 请求操作好友房间
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.REQ_OPERATE_FRIEND_ROOM
)
@ProtoDesc("请求操作好友房间")
public class ReqOperateFriendRoom {

    @ProtoDesc("房间ID")
    public long roomId;

    @ProtoDesc("操作类型 1. 暂停 2. 重新开启 3. 解散")
    public int operateCode;
}
