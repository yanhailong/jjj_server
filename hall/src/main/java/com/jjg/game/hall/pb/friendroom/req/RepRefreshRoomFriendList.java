package com.jjg.game.hall.pb.friendroom.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * 请求刷新房间好友列表
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.REQ_REFRESH_ROOM_FRIEND_LIST
)
@ProtoDesc("请求刷新房间好友列表")
public class RepRefreshRoomFriendList {

    @ProtoDesc("分页下标ID，从0开始，初始时请求下一页传1")
    public int pageIdx;

    @ProtoDesc("分页大小")
    public int pageSize;
}
