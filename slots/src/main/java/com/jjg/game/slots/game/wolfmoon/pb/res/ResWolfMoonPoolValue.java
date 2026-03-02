package com.jjg.game.slots.game.wolfmoon.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WOLF_MOON, cmd = WolfMoonConstant.MsgBean.RES_POOL_VALUE, resp = true)
@ProtoDesc("返回奖池结果")
public class ResWolfMoonPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResWolfMoonPoolValue(int code) {
        super(code);
    }
}
