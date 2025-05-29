package com.vegasnight.game.common.protostuff;

import java.lang.annotation.*;

/**
 * @since 1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Command {
    int value();
}
