package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 放弃任务返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_ABANDON_TASK, resp = true)
@ProtoDesc("放弃任务返回")
public class ResAbandonTask extends AbstractResponse {
    @ProtoDesc("再次接取冷却截止(ms)")
    public long cdUntil;

    public ResAbandonTask(int code) {
        super(code);
    }
}
