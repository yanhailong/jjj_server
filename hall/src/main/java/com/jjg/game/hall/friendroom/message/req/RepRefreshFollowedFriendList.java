package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 请求刷新房间好友列表
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.FRIEND_ROOM_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_REFRESH_ROOM_FRIEND_LIST
)
@ProtoDesc("请求刷新好友关注列表")
public class RepRefreshFollowedFriendList extends AbstractMessage {

    @ProtoDesc("分页下标ID，从0开始，初始时请求下一页传1")
    public int pageIdx;

    @ProtoDesc("分页大小")
    public int pageSize;
}
