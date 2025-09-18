package com.jjg.game.core.pb.reddot;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 更新小红点数据
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.NOTIFY_RED_DOT, resp = true)
@ProtoDesc("更新小红点数据")
public class NotifyRedDot extends AbstractNotice {

    @ProtoDesc("小红点数据详情列表")
    private List<RedDotDetails> redDotList;

    public List<RedDotDetails> getRedDotList() {
        return redDotList;
    }

    public void setRedDotList(List<RedDotDetails> redDotList) {
        this.redDotList = redDotList;
    }
}
