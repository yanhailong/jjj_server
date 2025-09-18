package com.jjg.game.core.pb.reddot;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 请求小红点信息
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.REQ_RED_DOT)
@ProtoDesc("请求小红点信息")
public class ReqRedDot extends AbstractMessage {

    /**
     * 单个模块的红点数据
     */
    @ProtoDesc("如果该参数有值,则推送指定模块红点数据,没有则推送所有红点数据")
    private RedDotDetails.RedDotModule module;

    @ProtoDesc("加载模块中对应子模块的红点数据,如果没有模块参数子模块不会被读取")
    private int submodule;

    public RedDotDetails.RedDotModule getModule() {
        return module;
    }

    public void setModule(RedDotDetails.RedDotModule module) {
        this.module = module;
    }

    public int getSubmodule() {
        return submodule;
    }

    public void setSubmodule(int submodule) {
        this.submodule = submodule;
    }
}
