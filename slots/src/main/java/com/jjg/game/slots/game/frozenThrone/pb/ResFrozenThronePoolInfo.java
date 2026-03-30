package com.jjg.game.slots.game.frozenThrone.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.frozenThrone.FrozenThroneConstant;

/**
 * @author lihaocao
 * @date 2025/12/2 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.FROZEN_THRONE, cmd = FrozenThroneConstant.MsgBean.RES_POOL_INFO, resp = true)
@ProtoDesc("返回奖池信息")
public class ResFrozenThronePoolInfo extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResFrozenThronePoolInfo(int code) {
        super(code);
    }
}
