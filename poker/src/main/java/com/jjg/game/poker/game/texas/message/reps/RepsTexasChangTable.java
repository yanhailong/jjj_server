package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.poker.game.texas.constant.TexasConstant;

/**
 * @author lm
 * @date 2025/8/5 16:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE,cmd = TexasConstant.MsgBean.REPS_CHANG_TABLE,resp = true)
@ProtoDesc("响应换桌")
public class RepsTexasChangTable extends AbstractResponse {
    public RepsTexasChangTable(int code) {
        super(code);
    }
}
