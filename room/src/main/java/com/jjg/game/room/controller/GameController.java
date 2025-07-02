package com.jjg.game.room.controller;


import com.jjg.game.core.constant.EGameType;

import java.lang.annotation.*;

/**
 * 游戏控制器注解
 *
 * @author 2CL
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface GameController {
    // 游戏类型
    EGameType gameType();
}
