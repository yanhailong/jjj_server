package com.jjg.game.common.proto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufMessage {
    byte node() default 0;

    boolean resp() default false;

    int messageType() default 0;

    int cmd() default 0;

    //是否要转成pb文件
    boolean toPbFile() default true;

}
