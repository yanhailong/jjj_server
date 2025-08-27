package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.struct.BaseFriendRoomPlayerInfo;

import java.util.List;

/**
 * 刷新关注好友列表
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_SHIELD_PLAYER_LIST,
    resp = true
)
@ProtoDesc("返回屏蔽玩家列表")
public class ResShieldPlayerList extends AbstractResponse {

    @ProtoDesc("屏蔽的玩家列表")
    public List<BaseFriendRoomPlayerInfo> shieldPlayerList;

    @ProtoDesc("可操作屏蔽玩家数量的上限")
    public int maxLimit;

    public ResShieldPlayerList(int code) {
        super(code);
    }
}
