package com.jjg.game.poker.game.tosouth.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;

/**
 * @author lm
 * @date 2025/8/5 16:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH,cmd = ToSouthConstant.MsgBean.REPS_CHANG_TABLE,resp = true)
@ProtoDesc("响应换桌")
public class RespToSouthChangTable extends AbstractResponse {
    public RespToSouthChangTable(int code) {
        super(code);
    }
}
