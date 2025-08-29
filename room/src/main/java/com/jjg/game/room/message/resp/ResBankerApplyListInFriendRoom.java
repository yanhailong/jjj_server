package com.jjg.game.room.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.message.RoomMessageConstant;
import com.jjg.game.room.message.struct.ApplyBankPlayerInfo;

import java.util.List;

/**
 * @author Administrator
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.ROOM_TYPE,
    cmd = RoomMessageConstant.RespMsgBean.RES_BANKER_APPLY_LIST,
    resp = true
)
@ProtoDesc("申请上庄列表")
public class ResBankerApplyListInFriendRoom extends AbstractResponse {

    @ProtoDesc("申请上庄的玩家信息")
    public List<ApplyBankPlayerInfo> applyBankPlayerInfos;

    @ProtoDesc("庄家信息")
    public ApplyBankPlayerInfo bankPlayerInfo;

    @ProtoDesc("连续坐庄次数")
    public int beBankerTimes;

    @ProtoDesc("最大连续坐庄次数")
    public int maxBankerTimes;

    public ResBankerApplyListInFriendRoom(int code) {
        super(code);
    }
}
