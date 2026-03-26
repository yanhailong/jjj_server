package com.jjg.game.ploy.controller;

import com.jjg.game.ploy.data.PlayerMultiPloyGameData;
import org.slf4j.Logger;

/**
 * 多人策略游戏抽象控制器
 *
 * @author 11
 * @date 2026/3/19
 */
public abstract class AbstractMultiPloyController<T extends PlayerMultiPloyGameData> extends AbstractPloyController<T> {
    public AbstractMultiPloyController(Logger log, Class<T> cla) {
        super(log, cla);
    }
}
