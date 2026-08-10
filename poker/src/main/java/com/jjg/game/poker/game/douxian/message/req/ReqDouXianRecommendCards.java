package com.jjg.game.poker.game.douxian.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE,
        cmd = DouXianConstant.MsgBean.REQ_DOU_XIAN_RECOMMEND_CARDS)
@ProtoDesc("DouXian request a recommendation for one zone")
public class ReqDouXianRecommendCards extends AbstractMessage {
    @ProtoDesc("Zone: 1=MORTAL, 2=SPIRIT, 3=IMMORTAL")
    public int zoneId;
}
