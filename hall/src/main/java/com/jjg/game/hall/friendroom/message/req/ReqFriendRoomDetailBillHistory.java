package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 请求好友房详细的账单历史
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_FRIEND_ROOM_DETAIL_BILL_HISTORY
)
@ProtoDesc("请求好友房详细账单历史")
public class ReqFriendRoomDetailBillHistory  extends AbstractMessage {

    @ProtoDesc("房间ID")
    public String roomId;

    @ProtoDesc("分页下标，从0开始")
    public int pageIdx;

    @ProtoDesc("分页大小")
    public int pageSize;
}
