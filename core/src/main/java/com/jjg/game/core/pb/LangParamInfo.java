package com.jjg.game.core.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/20 15:39
 */
@ProtobufMessage
@ProtoDesc("多语言参数")
public class LangParamInfo {
    @ProtoDesc("0.原始展示  1.多语言id")
    public int type;
    @ProtoDesc("参数")
    public String param;
}
