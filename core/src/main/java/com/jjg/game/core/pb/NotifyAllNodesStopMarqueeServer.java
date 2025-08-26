package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/13 10:03
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_STOP_MARQUEE_HALL_MASTER,resp = true, toPbFile = false)
@ProtoDesc("大厅主节点推送到其他节点停止跑马灯")
public class NotifyAllNodesStopMarqueeServer extends AbstractNotice {
    public int id;
}
