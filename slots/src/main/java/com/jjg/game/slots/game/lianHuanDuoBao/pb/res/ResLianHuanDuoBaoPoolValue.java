package com.jjg.game.slots.game.lianHuanDuoBao.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.lianHuanDuoBao.constant.LianHuanDuoBaoConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.LIAN_HUAN_DUO_BAO_TYPE, cmd = LianHuanDuoBaoConstant.MsgBean.RES_POOL_VALUE, resp = true)
@ProtoDesc("返回奖池")
public class ResLianHuanDuoBaoPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResLianHuanDuoBaoPoolValue(int code) {
        super(code);
    }
}
