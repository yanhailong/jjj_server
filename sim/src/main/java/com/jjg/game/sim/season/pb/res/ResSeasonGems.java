package com.jjg.game.sim.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.season.pb.struct.*;
import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SEASON_GEMS, resp = true)
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
