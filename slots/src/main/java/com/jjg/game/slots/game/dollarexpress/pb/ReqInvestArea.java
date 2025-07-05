package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/6/20 10:16
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE, cmd = SlotsConst.MsgBean.REQ_INVEST_AREA)
@ProtoDesc("选择投资地区")
public class ReqInvestArea extends AbstractMessage {
    @ProtoDesc("地区id1")
    public int areaId1;
    @ProtoDesc("地区id2")
    public int areaId2;
    @ProtoDesc("地区id3")
    public int areaId3;
}
