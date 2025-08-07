package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;

/**
 * @author 11
 * @date 2025/8/6 13:40
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_MARQUEE,resp = true, toPbFile = false)
@ProtoDesc("gm推送跑马灯")
public class ReqMarqueeServer extends AbstractResponse {
    public String content;

    public ReqMarqueeServer(int code) {
        super(code);
    }
}
