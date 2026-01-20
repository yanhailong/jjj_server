package com.jjg.game.activity.wealthroulette.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/12/1 10:15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_WEALTH_ROULETTE_HISTORY)
@ProtoDesc("财富轮盘历史记录")
public class ReqWealthRouletteHistory extends AbstractMessage {
    @ProtoDesc("起始索引")
    public int startIndex;
    @ProtoDesc("每页数目")
    public int size;
}
