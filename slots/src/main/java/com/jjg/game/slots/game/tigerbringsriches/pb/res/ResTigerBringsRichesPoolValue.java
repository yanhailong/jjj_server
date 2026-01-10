package com.jjg.game.slots.game.tigerbringsriches.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.tigerbringsriches.constant.TigerBringsRichesConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE, cmd = TigerBringsRichesConstant.MsgBean.RES_TIGER_BRINGS_RICHES_POOL_VALUE, resp = true)
@ProtoDesc("返回奖池")
public class ResTigerBringsRichesPoolValue extends AbstractResponse {
    public long major;

    public ResTigerBringsRichesPoolValue(int code) {
        super(code);
    }
}
