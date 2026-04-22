package com.jjg.game.poker.game.tosouthfree.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthfree.constant.ToSouthFreeConstant;

/**
 * @author lm
 * @date 2025/8/5 16:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH_FREE,cmd = ToSouthFreeConstant.MsgBean.REPS_CHANG_TABLE,resp = true)
@ProtoDesc("响应换桌")
public class RespToSouthFreeChangTable extends AbstractResponse {
    public RespToSouthFreeChangTable(int code) {
        super(code);
    }
}
