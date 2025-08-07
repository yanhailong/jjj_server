package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.message.bean.TexasHistory;

/**
 * @author lm
 * @date 2025/8/7 13:42
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE, cmd = TexasConstant.MsgBean.REQS_TEXAS_HISTORY,resp = true)
@ProtoDesc("请求德州扑克历史记录")
public class RepsTexasHistory extends AbstractResponse {
    @ProtoDesc("历史记录")
    public TexasHistory history;
    @ProtoDesc("最大记录数")
    public int maxRecodeNum;
    public RepsTexasHistory(int code) {
        super(code);
    }
}
