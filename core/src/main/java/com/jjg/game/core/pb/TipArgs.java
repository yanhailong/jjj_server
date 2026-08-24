package com.jjg.game.core.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.utils.TipUtils;

@ProtobufMessage
@ProtoDesc("提示参数")
public class TipArgs {
    /**
     * 参数类型 1=多语言id 2=多语言所需的替换参数
     * <p>
     * 类型常量定义{@link TipUtils.TipContextArgsType}
     */
    @ProtoDesc("参数类型 1=多语言id 2=多语言所需的替换参数")
    private int type;

    /**
     * 参数
     */
    @ProtoDesc("参数")
    private String arg;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getArg() {
        return arg;
    }

    public void setArg(String arg) {
        this.arg = arg;
    }
}
