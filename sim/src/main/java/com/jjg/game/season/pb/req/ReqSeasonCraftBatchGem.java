package com.jjg.game.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.REQ_SEASON_CRAFT_BATCH_GEM)
@ProtoDesc("批量合成赛季宝石")
public class ReqSeasonCraftBatchGem extends AbstractMessage {
    @ProtoDesc("要合成的宝石品质列表")
    public List<Integer> qualities;
}
