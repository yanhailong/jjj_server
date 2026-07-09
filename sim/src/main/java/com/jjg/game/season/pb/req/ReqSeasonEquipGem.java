package com.jjg.game.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.REQ_SEASON_EQUIP_GEM)
@ProtoDesc("镶嵌或卸下赛季宝石")
public class ReqSeasonEquipGem extends AbstractMessage {
    @ProtoDesc("槽位序号")
    public int slot;
    @ProtoDesc("宝石道具ID，0表示卸下")
    public int itemId;
}
