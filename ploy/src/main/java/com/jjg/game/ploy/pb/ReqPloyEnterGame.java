package com.jjg.game.ploy.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.constant.PloyConstant;

/**
 * @author 11
 * @date 2026/3/19
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_COMMON, cmd = PloyConstant.MsgBean.REQ_PLOY_ENTER_GAME)
@ProtoDesc("请求进入游戏")
public class ReqPloyEnterGame extends AbstractMessage {
    @ProtoDesc("游戏类型")
    public int gameType;
    @ProtoDesc("场次id")
    public int roomCfgId;
}
