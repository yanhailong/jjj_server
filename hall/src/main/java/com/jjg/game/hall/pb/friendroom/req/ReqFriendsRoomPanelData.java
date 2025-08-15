package com.jjg.game.hall.pb.friendroom.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * 请求好友房面板数据
 *
 * @author Administrator
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.REQ_FRIENDS_ROOM_PANEL_DATA
)
@ProtoDesc("请求好友房的面板数据")
public class ReqFriendsRoomPanelData extends AbstractMessage {
}
