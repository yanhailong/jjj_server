package com.jjg.game.hall.pb.friendroom.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * 返回操作房间好友列表
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.RES_OPERATE_ROOM_FRIENDS_LIST,
    resp = true
)
@ProtoDesc("返回操作房间好友列表")
public class ResOperateFollowedFriendsList extends AbstractResponse {

    public ResOperateFollowedFriendsList(int code) {
        super(code);
    }
}
