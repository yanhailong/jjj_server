package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;

import java.util.List;

/**
 * 通知功能开放，通过游戏中的某些操作触发了功能的开放
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE,
    cmd = MessageConst.CoreMessage.NOTIFY_FUNC_OPEN,
    resp = true
)
@ProtoDesc("通知功能开放，通过游戏中的某些操作触发了功能的开放")
public class NotifyOpenFunction extends AbstractNotice {

    @ProtoDesc("新增开放的功能ID列表")
    public List<Integer> functionIdList;
}
