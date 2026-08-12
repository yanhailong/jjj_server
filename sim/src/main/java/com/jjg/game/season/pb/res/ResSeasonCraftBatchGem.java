package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_CRAFT_BATCH_GEM, resp = true)
@ProtoDesc("批量合成赛季宝石返回")
public class ResSeasonCraftBatchGem extends AbstractResponse {
    @ProtoDesc("实际合成次数")
    public int craftCount;
    @ProtoDesc("合成成功次数")
    public int successCount;
    @ProtoDesc("实际扣除的宝石")
    public List<ItemInfo> consumedItems;
    @ProtoDesc("合成成功产出的宝石")
    public List<ItemInfo> resultItems;
    @ProtoDesc("合成失败保留的宝石")
    public List<ItemInfo> failKeepItems;
    @ProtoDesc("扣除合成费用后的赛季币")
    public long seasonCoin;

    public ResSeasonCraftBatchGem(int code) {
        super(code);
    }
}
