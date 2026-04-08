package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 通知生成南方前进牌库
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_GENERATE_TO_SOUTH_LIB, resp = true, toPbFile = false)
@ProtoDesc("通知生成南方前进牌库")
public class NotifyGenerateToSouthLib extends AbstractNotice {
    /** 生成局数 */
    public int count;
}
