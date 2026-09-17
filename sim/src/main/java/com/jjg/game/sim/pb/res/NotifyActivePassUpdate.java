package com.jjg.game.sim.pb.res;

import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.pb.struct.ActivePassTaskInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.NOTIFY_ACTIVE_PASS_UPDATE, resp = true)
@ProtoDesc("活跃通行证变更；reset时清空旧任务，否则按taskType和group替换任务")
public class NotifyActivePassUpdate extends AbstractResponse {
    public int passId;
    public int level;
    public long points;
    public int purchasedTracks;
    public boolean reset;
    public List<ActivePassTaskInfo> tasks;
    public NotifyActivePassUpdate(int code) { super(code); }
}
