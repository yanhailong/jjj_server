package com.jjg.game.core.messages;

import com.jjg.game.core.event.AbstractEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 消息实体包装类
 *
 * @author 2CL
 */
public class MessageEntityWrapper {

    /**
     * 通过动态确定提供的{@link AbstractEvent}实例的类型参数并创建相应类的新实例来包装消息实体。
     *
     * @param abstractEvent the {@link AbstractEvent} 其消息实体类型将被确定和实例化的实例
     * @return 与所提供事件的类型参数对应的消息实体类型的新实例
     */
    public static Object msgEntityWrapper(AbstractEvent<?> abstractEvent) {
        try {
            Class<?> msgEntityClass = getMsgEntityClass(abstractEvent);
            // 获取消息体的实例
            return msgEntityClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检索与提供的AbstractEvent实例关联的消息实体的类类型。此方法动态确定指定AbstractEvent实例的泛型类型参数，并加载相应的类。
     *
     * @param abstractEvent 从中确定消息实体类类型的AbstractEvent实例
     * @return Class对象表示所提供的AbstractEvent的消息实体类型
     */
    public static Class<?> getMsgEntityClass(AbstractEvent<?> abstractEvent) {
        Type type = abstractEvent.getClass().getGenericSuperclass();
        if (!(type instanceof ParameterizedType parameterizedType)) {
            throw new RuntimeException("获取消息事件类的消息实体类型失败");
        }
        Type[] parameterizedTypes = parameterizedType.getActualTypeArguments();
        Type msgEntityClassType = parameterizedTypes[0];
        try {
            String classTypeName = msgEntityClassType.getTypeName();
            return Class.forName(classTypeName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
