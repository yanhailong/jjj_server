package com.jjg.game.poker.game.common.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.poker.game.common.constant.PokerConstant;

/**
 * @author lm
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.POKER_GENERAL_TYPE,
        cmd = PokerConstant.MsgBean.REQ_ROOM_BASE_INFO)
@ProtoDesc("请求房间初始信息")
public class ReqPokerRoomBaseInfo extends AbstractMessage {

}
