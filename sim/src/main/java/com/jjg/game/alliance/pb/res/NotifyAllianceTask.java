package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 联盟任务完成/失败通知。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.NOTIFY_TASK, resp = true)
@ProtoDesc("联盟任务完成/失败通知")
public class NotifyAllianceTask extends AbstractResponse {
    @ProtoDesc("1完成 2超时失败")
    public int result;
    @ProtoDesc("任务配置id")
    public int cfgId;

    public NotifyAllianceTask(int code) {
        super(code);
    }
}
