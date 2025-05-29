package com.vegasnight.game.core.manager;

import com.vegasnight.game.common.monitor.FileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author 11
 * @date 2025/5/27 10:14
 */
@Component
public class GameConfigManager implements FileLoader {
    private Logger log = LoggerFactory.getLogger(getClass());

    public void init() {
        log.debug("开始加载配置...");
    }

    @Override
    public void load(File file, boolean isNew) {

    }
}
