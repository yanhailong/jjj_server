package com.jjg.game.common.protostuff;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消息类型注解
 *
 * @author nobody
 * @since 1.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageType {
    /**
     * 消息类型ID
     *
     * @return 类型ID
     */
    int value();

    /**
     * 是否是分组消息
     */
    boolean isGroupMessage() default false;
}
