package com.jjg.game.core.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/20 16:16
 */
@ProtobufMessage
@ProtoDesc("跑马灯信息")
public class MarqueeInfo {
    public int id;
    @ProtoDesc("内容")
    public LanguageInfo content;
    @ProtoDesc("播放时间")
    public int showTime;
    @ProtoDesc("间隔时间")
    public int interval;
    @ProtoDesc("开始时间")
    public int startTime;
    @ProtoDesc("结束时间")
    public int endTime;
}
