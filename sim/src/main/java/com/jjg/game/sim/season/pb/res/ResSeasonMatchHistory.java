package com.jjg.game.sim.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.season.pb.struct.SeasonMatchRecordInfo;
import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SEASON_MATCH_HISTORY, resp = true)
@ProtoDesc("赛季对局记录返回")
public class ResSeasonMatchHistory extends AbstractResponse {
    @ProtoDesc("按完成时间倒序的对局记录")
    public List<SeasonMatchRecordInfo> records;

    public ResSeasonMatchHistory(int code) {
        super(code);
    }
}
