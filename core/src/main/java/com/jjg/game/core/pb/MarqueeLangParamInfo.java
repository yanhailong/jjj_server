package com.jjg.game.core.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/14 18:13
 */
@ProtobufMessage
@ProtoDesc("跑马灯中多语言参数")
public class MarqueeLangParamInfo {
    @ProtoDesc("0.原始展示  1.多语言id")
    public int type;
    @ProtoDesc("参数")
    public String param;
}
