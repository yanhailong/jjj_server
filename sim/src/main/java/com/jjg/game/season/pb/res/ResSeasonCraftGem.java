package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_CRAFT_GEM, resp = true)
@ProtoDesc("合成赛季宝石返回(第一步)。success=false 表示合成失败，需再发起 ReqSeasonCraftGemKeep 选择保留的宝石")
public class ResSeasonCraftGem extends AbstractResponse {
    @ProtoDesc("是否合成成功")
    public boolean success;
    @ProtoDesc("成功产出的宝石道具ID")
    public int resultItemId;
    @ProtoDesc("成功产出数量")
    public int resultCount;
    @ProtoDesc("扣除合成费用后的赛季币")
    public long seasonCoin;

    public ResSeasonCraftGem(int code) {
        super(code);
    }
}
