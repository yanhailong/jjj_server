package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.VisitRecordInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_VISIT_RECORDS, resp = true)
@ProtoDesc("拜访记录返回")
public class ResVisitRecords extends AbstractResponse {
    public int total;
    public List<VisitRecordInfo> records;
    public int todayPopularity;
    public long totalPopularity;

    public ResVisitRecords(int code) {
        super(code);
    }
}
