package com.jjg.game.slots.game.lianHuanDuoBao.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.lianHuanDuoBao.constant.LianHuanDuoBaoConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.LIAN_HUAN_DUO_BAO_TYPE, cmd = LianHuanDuoBaoConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqLianHuanDuoBaoStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
    @ProtoDesc("注数乘积（1-5）")
    public int stakeMultiplier;
}
