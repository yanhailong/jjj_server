package com.jjg.game.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.REQ_SEASON_CRAFT_GEM)
@ProtoDesc("合成赛季宝石(第一步:发起合成)")
public class ReqSeasonCraftGem extends AbstractMessage {
    @ProtoDesc("作为材料的宝石道具ID列表")
    public List<Integer> itemIds;
}
