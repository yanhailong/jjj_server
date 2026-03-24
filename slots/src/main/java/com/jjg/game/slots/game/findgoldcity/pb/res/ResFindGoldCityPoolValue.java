package com.jjg.game.slots.game.findgoldcity.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.findgoldcity.constant.FindGoldCityConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.FIND_GOLD_CITY, cmd = FindGoldCityConstant.MsgBean.RES_FIND_GOLD_CITY_POOL_VALUE, resp = true)
@ProtoDesc("返回奖池")
public class ResFindGoldCityPoolValue extends AbstractResponse {
    public long major;

    public ResFindGoldCityPoolValue(int code) {
        super(code);
    }
}
