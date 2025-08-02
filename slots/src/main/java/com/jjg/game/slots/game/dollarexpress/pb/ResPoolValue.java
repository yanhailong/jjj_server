package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;

/**
 * @author 11
 * @date 2025/8/1 15:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE, cmd = DollarExpressConstant.MsgBean.RES_POOL_VALUE,resp = true)
@ProtoDesc("返回奖池结果")
public class ResPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResPoolValue(int code) {
        super(code);
    }
}
