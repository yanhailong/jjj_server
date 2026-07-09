package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;
import com.jjg.game.season.pb.struct.SeasonGemSlotInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_EQUIP_GEM, resp = true)
@ProtoDesc("镶嵌赛季宝石返回")
public class ResSeasonEquipGem extends AbstractResponse {
    @ProtoDesc("更新后的槽位")
    public List<SeasonGemSlotInfo> slots;

    public ResSeasonEquipGem(int code) {
        super(code);
    }
}
