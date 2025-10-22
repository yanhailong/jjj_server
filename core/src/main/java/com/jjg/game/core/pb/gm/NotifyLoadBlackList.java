package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/10/22 15:22
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTIFY_LOAD_BLACK_LIST, resp = true, toPbFile = false)
@ProtoDesc("通知黑名单")
public class NotifyLoadBlackList extends AbstractNotice {
    public boolean loadId;
    public boolean loadIp;
}
