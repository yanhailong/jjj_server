package com.jjg.game.slots.game.findgoldcity.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.findgoldcity.constant.FindGoldCityConstant;


@ProtobufMessage(messageType = MessageConst.MessageTypeDef.FIND_GOLD_CITY, cmd = FindGoldCityConstant.MsgBean.REQ_FIND_GOLD_CITY_POOL_VALUE)
@ProtoDesc("请求奖池")
public class ReqFindGoldCityPoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
