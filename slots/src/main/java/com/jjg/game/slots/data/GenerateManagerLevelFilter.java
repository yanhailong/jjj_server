package com.jjg.game.slots.data;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

public class GenerateManagerLevelFilter extends TurboFilter {

    @Override
    public FilterReply decide(
            Marker marker,
            Logger logger,
            Level level,
            String format,
            Object[] params,
            Throwable t) {

        String loggerName = logger.getName();

        // 只命中 XXXGenerateManager
        if (loggerName.endsWith("GenerateManager")) {
            // 只允许 ERROR 及以上
            if (level.isGreaterOrEqual(Level.ERROR)) {
                return FilterReply.NEUTRAL;
            } else {
                return FilterReply.DENY;
            }
        }

        // 其他 logger 不受影响
        return FilterReply.NEUTRAL;
    }
}

