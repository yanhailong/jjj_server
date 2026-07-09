package com.jjg.game.sim.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_SEASON_CRAFT_GEM)
@ProtoDesc("合成赛季宝石")
public class ReqSeasonCraftGem extends AbstractMessage {
    @ProtoDesc("作为材料的宝石道具ID列表")
    public List<Integer> itemIds;
    @ProtoDesc("合成失败时选择保留的宝石道具ID")
    public int keepItemId;
}
