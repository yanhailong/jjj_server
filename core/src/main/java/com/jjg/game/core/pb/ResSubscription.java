package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 请求订阅消息主题回复
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.RES_SUBSCRIBE_TOPIC, resp = true)
@ProtoDesc("请求订阅消息主题回复")
public class ResSubscription extends AbstractResponse {

    /**
     * 主题名称
     */
    @ProtoDesc("主题名称")
    private String topic;

    /**
     * true 订阅,false 取消订阅
     */
    @ProtoDesc("true 订阅,false 取消订阅")
    private boolean subscription;

    public ResSubscription(int code) {
        super(code);
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public boolean isSubscription() {
        return subscription;
    }

    public void setSubscription(boolean subscription) {
        this.subscription = subscription;
    }
}
