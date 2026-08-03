package com.jjg.game.core.service;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.dao.CountDao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 提供全服统一的开服时间。Redis 只在进程首次访问时读取，之后直接使用内存值。
 */
@Service
public class ServerOpenTimeService {
    private static final String OPEN_SERVER_TIME = "openServerTime";

    private final CountDao countDao;
    private volatile long serverOpenTimeSeconds;

    public ServerOpenTimeService(CountDao countDao) {
        this.countDao = countDao;
    }

    public long getServerOpenTimeSeconds() {
        long cached = serverOpenTimeSeconds;
        if (cached > 0) {
            return cached;
        }
        synchronized (this) {
            if (serverOpenTimeSeconds > 0) {
                return serverOpenTimeSeconds;
            }
            long today = TimeHelper.getCurrentDateZeroSecondTime();
            boolean initialized = countDao.setIfAbsent(CountDao.CountType.SYSTEM.getParam(),
                    OPEN_SERVER_TIME, BigDecimal.valueOf(today));
            serverOpenTimeSeconds = initialized ? today : countDao.getCount(
                    CountDao.CountType.SYSTEM.getParam(), OPEN_SERVER_TIME).longValue();
            return serverOpenTimeSeconds;
        }
    }
}
