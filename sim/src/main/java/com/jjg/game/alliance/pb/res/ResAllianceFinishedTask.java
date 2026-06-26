package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.pb.struct.AllianceTaskInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 已完成的联盟任务列表返回
 *
 * @author 11
 * @date 2026/6/26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_FINISHED_TASK, resp = true)
@ProtoDesc("已完成的联盟任务列表返回")
public class ResAllianceFinishedTask extends AbstractResponse {
    @ProtoDesc("已完成任务列表(最新在前)")
    public List<AllianceTaskInfo> tasks;

    public ResAllianceFinishedTask(int code) {
        super(code);
    }
}
