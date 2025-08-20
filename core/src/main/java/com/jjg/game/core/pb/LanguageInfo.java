package com.jjg.game.core.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/20 15:38
 */
@ProtobufMessage
@ProtoDesc("多语言")
public class LanguageInfo {
    @ProtoDesc("0.原始展示  1.多语言参数匹配")
    public int type;
    @ProtoDesc("内容")
    public String content;
    @ProtoDesc("多语言id")
    public int langId;
    @ProtoDesc("参数")
    public List<LangParamInfo> params;
}
