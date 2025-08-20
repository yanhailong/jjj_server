package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.struct.FriendRoomBillPlayerInfo;

import java.util.List;

/**
 * 返回账单中玩家信息
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_FRIEND_ROOM_BILL_PLAYER_INFO,
    resp = true
)
@ProtoDesc("返回好友房详细账单历史")
public class ResFriendRoomBillPlayerInfo extends AbstractResponse {

    @ProtoDesc("好友房账单中玩家数据信息")
    public List<FriendRoomBillPlayerInfo> playerInfos;

    public ResFriendRoomBillPlayerInfo(int code) {
        super(code);
    }
}
