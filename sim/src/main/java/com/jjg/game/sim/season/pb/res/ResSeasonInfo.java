package com.jjg.game.sim.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.season.pb.struct.SeasonInfo;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SEASON_INFO, resp = true)
@ProtoDesc("当前赛季信息返回")
public class ResSeasonInfo extends AbstractResponse {
    @ProtoDesc("当前赛季信息")
    public SeasonInfo info;

    public ResSeasonInfo(int code) {
        super(code);
    }
}
