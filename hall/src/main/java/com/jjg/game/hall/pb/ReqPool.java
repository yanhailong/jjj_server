package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/7 10:04
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_POOL)
@ProtoDesc("请求倍场界面奖池信息")
public class ReqPool extends AbstractMessage {
    @ProtoDesc("游戏类型")
    public int gameType;
}
