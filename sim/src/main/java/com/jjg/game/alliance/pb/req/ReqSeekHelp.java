package com.jjg.game.alliance.pb.req;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 发起求助。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.REQ_SEEK_HELP)
@ProtoDesc("发起求助")
public class ReqSeekHelp extends AbstractMessage {
    @ProtoDesc("1任务求助 2建筑加速")
    public int type;
    @ProtoDesc("目标id(任务uid/建筑id)")
    public long targetId;
    @ProtoDesc("目标展示名(前端卡片用)")
    public String targetName;
}
