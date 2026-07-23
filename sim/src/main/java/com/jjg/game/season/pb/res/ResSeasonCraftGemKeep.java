package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_CRAFT_GEM_KEEP, resp = true)
@ProtoDesc("合成赛季宝石返回(第二步:确认保留宝石并结算失败消耗)")
public class ResSeasonCraftGemKeep extends AbstractResponse {
    @ProtoDesc("实际保留的宝石道具ID")
    public int keptItemId;
    @ProtoDesc("结算后的赛季币")
    public long seasonCoin;

    public ResSeasonCraftGemKeep(int code) {
        super(code);
    }
}
