package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.message.bean.DouXianRecommendZoneInfo;
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE,
        cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_RECOMMEND_CARDS, resp = true)
@ProtoDesc("DouXian recommended card arrangement notification")
public class NotifyDouXianRecommendCards extends AbstractNotice {
    @ProtoDesc("Current round")
    public int round;
    @ProtoDesc("Recommendation for the clicked zone, visible only to the owning player")
    public DouXianRecommendZoneInfo zone;
}
