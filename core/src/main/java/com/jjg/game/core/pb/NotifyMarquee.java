package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/6 13:58
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE,
        cmd = MessageConst.CoreMessage.NOTICE_MARQUEE, resp = true)
@ProtoDesc("通知跑马灯信息")
public class NotifyMarquee extends AbstractNotice{
    public long id;
    @ProtoDesc("0.原始展示  1.多语言参数匹配")
    public int type;
    @ProtoDesc("内容")
    public String content;
    @ProtoDesc("播放时间")
    public int showTime;
    @ProtoDesc("间隔时间")
    public int interval;
    @ProtoDesc("开始时间")
    public int startTime;
    @ProtoDesc("结束时间")
    public int endTime;
    @ProtoDesc("多语言id")
    public int langId;
    @ProtoDesc("参数")
    public List<MarqueeLangParamInfo> params;
}
