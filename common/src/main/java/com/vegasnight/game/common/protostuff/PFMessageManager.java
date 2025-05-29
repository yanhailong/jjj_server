package com.vegasnight.game.common.protostuff;

import com.vegasnight.game.common.proto.ProtobufMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * PF 消息管理器，将所有注解为@ProtobufMessage的class加载出来，并做好映射关系
 *
 * @since 1.0
 */
public class PFMessageManager {
    public static Map<Class<?>, ProtobufMessage> responseMap;

    public static Logger log = LoggerFactory.getLogger(PFMessageManager.class);

    public static PFMessage getPFMessage(Object msg) {
        ProtobufMessage responseMessage = responseMap.get(msg.getClass());
        if (responseMessage == null) {
            log.warn("消息发送失败，该消息结构没有被ResponseMessage注解，msg-class={}", msg.getClass());
            return null;
        }
        byte[] data = ProtostuffUtil.serialize(msg);
        PFMessage pfMessage = new PFMessage(responseMessage.cmd(), data);
        return pfMessage;
    }

}
