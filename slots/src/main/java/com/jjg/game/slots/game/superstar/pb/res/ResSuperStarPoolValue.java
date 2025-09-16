package com.jjg.game.slots.game.superstar.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.superstar.SuperStarConstant;

/**
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SUPER_STAR_TYPE, cmd = SuperStarConstant.MsgBean.RES_POOL_VALUE,resp = true)
@ProtoDesc("返回奖池结果")
public class ResSuperStarPoolValue extends AbstractResponse {

    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResSuperStarPoolValue(int code) {
        super(code);
    }
}
