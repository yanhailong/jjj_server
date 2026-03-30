package com.jjg.game.slots.game.hotfootball.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.slots.game.hotfootball.HotFootballConstant;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HOT_FOOTBALL_TYPE, cmd = HotFootballConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqHotFootballStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
