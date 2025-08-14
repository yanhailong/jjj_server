package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/13 15:34
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.RES_MARQUEE,resp = true)
@ProtoDesc("返回当前跑马灯")
public class ResMarquee extends AbstractResponse{
    public long id;
    @ProtoDesc("0.原始展示  1.多语言参数匹配")
    public int type;
    @ProtoDesc("内容")
    public String content;
    @ProtoDesc("间隔时间")
    public int interval;
    @ProtoDesc("开始时间")
    public int startTime;
    @ProtoDesc("结束时间")
    public int endTime;
    @ProtoDesc("多语言id")
    public int langId;
    @ProtoDesc("参数")
    public List<String> params;

    public ResMarquee(int code) {
        super(code);
    }
}
