package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 通知配置变化
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE,
        cmd = MessageConst.ToServer.CONFIG_UPDATE, resp = true, toPbFile = false)
public class NotifyConfigUpdate {

    /**
     * 变化的配置名字
     */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
