package com.jjg.game.hall.pb.friendroom.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.friendroom.struct.FriendRoomBaseData;
import com.jjg.game.hall.pb.friendroom.struct.FriendRoomDetailData;

/**
 * 返回好友房详细数据
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.RES_FRIEND_ROOM_DETAIL_INFO,
    resp = true
)
@ProtoDesc("返回好友房详细数据")
public class ResFriendRoomDetailInfo extends AbstractResponse {

    @ProtoDesc("好友房基础数据")
    public FriendRoomBaseData friendRoomBaseData;

    @ProtoDesc("好友房详细信息")
    public FriendRoomDetailData friendRoomDetailData;

    public ResFriendRoomDetailInfo(int code) {
        super(code);
    }
}
