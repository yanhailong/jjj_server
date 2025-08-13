package com.jjg.game.hall.pb.friendroom.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.friendroom.struct.GameBillInfo;

import java.util.List;

/**
 * 返回好友房账单历史
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.RES_FRIEND_ROOM_BILL_HISTORY,
    resp = true
)
@ProtoDesc("返回好友房详细数据")
public class ResFriendRoomBillHistory extends AbstractResponse {

    @ProtoDesc("游戏账单数据")
    public List<GameBillInfo> gameBillInfos;

    public ResFriendRoomBillHistory(int code) {
        super(code);
    }
}
