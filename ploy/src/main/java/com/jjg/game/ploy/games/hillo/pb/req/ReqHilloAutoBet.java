package com.jjg.game.ploy.games.hillo.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.REQ_HILLO_AUTO_BET)
@ProtoDesc("HILLO auto bet")
public class ReqHilloAutoBet extends AbstractMessage {
    @ProtoDesc("bet amount")
    public long bet;
    @ProtoDesc("single round guess times, 1-50")
    public int guessTimes;
    @ProtoDesc("auto round times, 0 means infinite")
    public int betTimes;
}
