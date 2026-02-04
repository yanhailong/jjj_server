package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/12 10:54
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_MARQUEE_HALL_MASTER,resp = true, toPbFile = false)
@ProtoDesc("推送到其他节点跑马灯")
public class NotifyAllNodesMarqueeServer extends AbstractNotice {
    @ProtoDesc("跑马灯信息")
    public MarqueeInfo marqueeInfo;
    @ProtoDesc("跑马灯类型")
    public int type;
    @ProtoDesc("优先级")
    public int priority;
}
