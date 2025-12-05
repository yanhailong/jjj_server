package com.jjg.game.slots.game.thor.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.thor.ThorConstant;

/**
 * @author 11
 * @date 2025/12/1 18:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.THOR, cmd = ThorConstant.MsgBean.REQ_FREE_CHOOSE_ONE, resp = true)
@ProtoDesc("返回免费模式二选一")
public class ResThorFreeChooseOne extends AbstractResponse {
    public ResThorFreeChooseOne(int code) {
        super(code);
    }
}
