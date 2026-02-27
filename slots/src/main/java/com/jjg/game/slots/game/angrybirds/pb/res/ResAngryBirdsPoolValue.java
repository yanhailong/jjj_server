package com.jjg.game.slots.game.angrybirds.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.angrybirds.constant.AngryBirdsConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ANGRY_BIRDS, cmd = AngryBirdsConstant.MsgBean.RES_ANGRY_BIRDS_POOL_VALUE, resp = true)
@ProtoDesc("返回奖池")
public class ResAngryBirdsPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResAngryBirdsPoolValue(int code) {
        super(code);
    }
}
