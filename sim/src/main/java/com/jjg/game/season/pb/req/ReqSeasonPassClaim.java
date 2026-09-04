package com.jjg.game.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.REQ_SEASON_PASS_CLAIM)
@ProtoDesc("一键领取指定通行证已达成且已解锁的奖励")
public class ReqSeasonPassClaim extends AbstractMessage {
    @ProtoDesc("PassList.id")
    public int passId;
}
