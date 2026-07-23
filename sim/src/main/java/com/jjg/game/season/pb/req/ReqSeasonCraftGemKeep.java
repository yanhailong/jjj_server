package com.jjg.game.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.REQ_SEASON_CRAFT_GEM_KEEP)
@ProtoDesc("合成赛季宝石(第二步:合成失败后选择保留的宝石)")
public class ReqSeasonCraftGemKeep extends AbstractMessage {
    @ProtoDesc("选择保留的宝石道具ID(必须是本次合成的材料之一)")
    public int keepItemId;
}
