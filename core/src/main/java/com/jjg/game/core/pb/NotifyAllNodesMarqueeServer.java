package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/12 10:54
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_MARQUEE_HALL_MASTER,resp = true, toPbFile = false)
@ProtoDesc("推送到其他节点跑马灯")
public class NotifyAllNodesMarqueeServer extends AbstractNotice {
    public int id;
    @ProtoDesc("内容")
    public String content;
    @ProtoDesc("播放时间")
    public int showTime;
    @ProtoDesc("间隔时间")
    public int interval;
    @ProtoDesc("跑马灯类型")
    public int type;
    @ProtoDesc("开始时间")
    public int startTime;
    @ProtoDesc("结束时间")
    public int endTime;
}
