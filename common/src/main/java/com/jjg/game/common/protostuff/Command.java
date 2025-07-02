package com.jjg.game.common.protostuff;

import java.lang.annotation.*;

/**
 * 消息ID注解
 *
 * @author nobody
 * @since 1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Command {
    /**
     * 指令ID
     */
    int value();

    /**
     * 是否是分组消息处理器
     */
    boolean isGroupMsgDispatcher() default false;
}
