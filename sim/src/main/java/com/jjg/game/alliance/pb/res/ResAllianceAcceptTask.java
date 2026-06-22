package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceTaskInfo;

/**
 * 接取任务返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_ACCEPT_TASK, resp = true)
@ProtoDesc("接取任务返回")
public class ResAllianceAcceptTask extends AbstractResponse {
    @ProtoDesc("已接取的任务")
    public AllianceTaskInfo task;

    public ResAllianceAcceptTask(int code) {
        super(code);
    }
}
