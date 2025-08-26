package com.jjg.game.poker.game.texas.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.poker.game.texas.constant.TexasConstant;

/**
 * @author lm
 * @date 2025/8/7 13:42
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE, cmd = TexasConstant.MsgBean.REQ_TEXAS_HISTORY)
@ProtoDesc("请求德州扑克历史记录")
public class ReqTexasHistory extends AbstractMessage {
    @ProtoDesc("索引")
    public int index;
}
