package com.jjg.game.slots.game.tenfoldgoldenbull.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.tenfoldgoldenbull.constant.TenFoldGoldenBullConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE, cmd = TenFoldGoldenBullConstant.MsgBean.RES_TEN_FOLD_GOLDEN_BULL_POOL_VALUE, resp = true)
@ProtoDesc("返回奖池")
public class ResTenFoldGoldenBullPoolValue extends AbstractResponse {
    public long major;

    public ResTenFoldGoldenBullPoolValue(int code) {
        super(code);
    }
}
