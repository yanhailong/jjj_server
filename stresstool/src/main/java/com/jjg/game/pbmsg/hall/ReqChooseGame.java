package com.jjg.game.pbmsg.hall;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;

/**
 * @author 11
 * @date 2025/6/10 16:58
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = StressMsgConst.HallMsgBean.REQ_ENTER_GAME)
@ProtoDesc("请求进入游戏")
public class ReqChooseGame extends AbstractMessage {
    @ProtoDesc("游戏类型")
    public int gameType;
}
