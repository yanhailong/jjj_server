package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;
import com.jjg.game.season.pb.struct.SeasonGemInfo;
import com.jjg.game.season.pb.struct.SeasonGemSlotInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_GEMS, resp = true)
@ProtoDesc("赛季宝石列表返回")
public class ResSeasonGems extends AbstractResponse {
    @ProtoDesc("宝石列表")
    public List<SeasonGemInfo> gems;
    @ProtoDesc("镶嵌槽位")
    public List<SeasonGemSlotInfo> slots;

    public ResSeasonGems(int code) {
        super(code);
    }
}
