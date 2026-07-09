package com.jjg.game.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.REQ_SEASON_BUY)
@ProtoDesc("购买赛季商店商品")
public class ReqSeasonBuy extends AbstractMessage {
    @ProtoDesc("商品配置ID")
    public int shopId;
    @ProtoDesc("购买数量")
    public int count;
}
