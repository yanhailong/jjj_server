package com.jjg.game.poker.game.texas.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.poker.game.texas.constant.TexasConstant;

/**
 * @author lm
 * @date 2025/7/26 11:53
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE, cmd = TexasConstant.MsgBean.REQ_CHANGE_SEAT_STATE)
@ProtoDesc("请求坐下或站起")
public class ReqTexasChangeSeatState extends AbstractMessage {
    @ProtoDesc("类型 1改变站起坐下 2.改变座位位置")
    public int changeType;
    @ProtoDesc("参数（类型1 时为1坐下 2站起 类型2为座位id）")
    public int param;
}
