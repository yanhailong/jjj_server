package com.jjg.game.ploy.controller;

import com.jjg.game.ploy.data.PlayerSinglePloyGameData;
import org.slf4j.Logger;

/**
 * 单人策略游戏抽象控制器
 *
 * @author 11
 * @date 2026/3/19
 */
public abstract class AbstractSinglePloyController<T extends PlayerSinglePloyGameData> extends AbstractPloyController<T> {
    public AbstractSinglePloyController(Logger log, Class<T> gameDataClass) {
        super(log, gameDataClass);
    }
}
