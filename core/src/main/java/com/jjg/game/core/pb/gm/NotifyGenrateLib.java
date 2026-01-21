package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/13 12:20
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_GENERATE_LIB, resp = true,toPbFile = false)
@ProtoDesc("通知生成结果库")
public class NotifyGenrateLib extends AbstractNotice {
    public List<KVInfo> list;
}
