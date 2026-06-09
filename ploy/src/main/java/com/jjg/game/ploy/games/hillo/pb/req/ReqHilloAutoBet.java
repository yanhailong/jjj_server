package com.jjg.game.ploy.games.hillo.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.REQ_HILLO_AUTO_BET)
@ProtoDesc("HILLO 开始自动投注，返回自动投注状态")
public class ReqHilloAutoBet extends AbstractMessage {
    @ProtoDesc("每局下注金额")
    public long bet;
    @ProtoDesc("单局自动猜测次数，范围 1-50")
    public int guessTimes;
    @ProtoDesc("自动投注局数，0 表示无限局")
    public int betTimes;
}
