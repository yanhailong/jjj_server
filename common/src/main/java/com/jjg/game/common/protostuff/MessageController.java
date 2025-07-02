package com.jjg.game.common.protostuff;

import com.esotericsoftware.reflectasm.MethodAccess;

import java.util.Map;

/**
 * 消息控制器
 *
 * @author nobody
 * @since 1.0
 */
public class MessageController {
    // 消息类型对应的SpringBean实例
    public Object been;
    // 方法反射类
    public MethodAccess methodAccess;
    public Map<Integer, MethodInfo> MethodInfos;
    // 控制器的注解类
    private MessageType messageType;


    public MessageController(Object been) {
        this.been = been;
        Class<?> clazz = been.getClass();
        methodAccess = MethodAccess.get(clazz);
        MethodInfos = MessageUtil.load(methodAccess, clazz);
    }

    public MessageController(Object been, Class<?> clazz) {
        this.been = been;
        //Class<?> clazz = been.getClass();
        methodAccess = MethodAccess.get(clazz);
        MethodInfos = MessageUtil.load(methodAccess, clazz);
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}
