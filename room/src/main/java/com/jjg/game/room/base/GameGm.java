package com.jjg.game.room.base;

import java.lang.annotation.*;

/**
 * 游戏gm注解
 *
 * @author 2CL
 */
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface GameGm {

    /**
     * gm命令
     */
    String cmd();
}
