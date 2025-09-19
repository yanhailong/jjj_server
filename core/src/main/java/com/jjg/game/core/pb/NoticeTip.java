package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.utils.TipUtils;

import java.util.List;

/**
 * 通知客户端多语言弹窗
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE,
        cmd = MessageConst.CoreMessage.NOTIFY_TIP,
        resp = true
)
@ProtoDesc("通知客户端多语言提示")
public class NoticeTip extends AbstractNotice {

    /**
     * 弹窗类型
     * <p>
     * 类型常量定义{@link TipUtils.TipType}
     */
    @ProtoDesc("弹窗类型")
    private int tipType;

    /**
     * 多语言id
     */
    @ProtoDesc("多语言id")
    private long languageId;

    /**
     * 参数
     */
    @ProtoDesc("参数")
    private List<TipArgs> tipArgs;

    /**
     * 提示参数
     */
    @ProtobufMessage
    @ProtoDesc("提示参数")
    public static class TipArgs {
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

    public int getTipType() {
        return tipType;
    }

    public void setTipType(int tipType) {
        this.tipType = tipType;
    }

    public long getLanguageId() {
        return languageId;
    }

    public void setLanguageId(long languageId) {
        this.languageId = languageId;
    }

    public List<TipArgs> getTipArgs() {
        return tipArgs;
    }

    public void setTipArgs(List<TipArgs> tipArgs) {
        this.tipArgs = tipArgs;
    }
}
