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
