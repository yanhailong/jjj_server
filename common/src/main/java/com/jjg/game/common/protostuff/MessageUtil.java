package com.jjg.game.common.protostuff;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @since 1.0
 */
public class MessageUtil {

    static Logger log = LoggerFactory.getLogger(MessageUtil.class);
    public static Map<Class<?>, ProtobufMessage> responseMap;

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

    public static Map<Integer, MessageController> load(ApplicationContext context) {
        Map<Integer, MessageController> messageControllers = new HashMap<>();
        Class<MessageType> clazz = MessageType.class;
        log.debug("开始初始化 {} 消息分发器", clazz);
        Map<String, Object> beans = context.getBeansWithAnnotation(clazz);
        beans.values().forEach(o -> {

            MessageType messageType = null;
            if (AopUtils.isAopProxy(o)) {
                Class<?> targetclazz = AopUtils.getTargetClass(o);
                messageType = targetclazz.getAnnotation(MessageType.class);
                MessageController messageController = new MessageController(o, targetclazz);
                messageControllers.put(messageType.value(), messageController);
            } else {
                messageType = o.getClass().getAnnotation(MessageType.class);
                MessageController messageController = new MessageController(o);
                messageControllers.put(messageType.value(), messageController);
            }

        });
        return messageControllers;
    }

    public static Map<Integer, MethodInfo> load(MethodAccess methodAccess, Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        Map<Integer, MethodInfo> methodInfos = new HashMap<>();
        if (methods != null && methods.length >= 0) {
            for (Method method : methods) {
                Class<Command> clz = Command.class;
                Command command = method.getAnnotation(clz);
                if (command != null) {
                    String name = method.getName();
                    Class[] types = method.getParameterTypes();
                    Type returnType = method.getReturnType();
                    int index = methodAccess.getIndex(name, types);
                    MethodInfo methodInfo = new MethodInfo(index, name, types, returnType);
                    methodInfos.put(command.value(), methodInfo);
                }

            }
        }
        return methodInfos;
    }

    public static Map<Class<?>, ProtobufMessage> loadResponseMessage(String... pkgs) {
        responseMap = new HashMap<>();
        Set<Class<?>> clazzes = new HashSet<>();
        for (String pkg : pkgs) {
            clazzes.addAll(ClassUtils.getAllClassByAnnotation(pkg, ProtobufMessage.class));
        }
        if (!clazzes.isEmpty()) {
            clazzes.forEach(clazz -> {
                ProtobufMessage responseMessage = clazz.getAnnotation(ProtobufMessage.class);
                if (responseMessage.resp()) {
                    responseMap.put(clazz, responseMessage);
                }
            });
        }
        return responseMap;
    }
}
