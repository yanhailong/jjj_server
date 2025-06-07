package com.vegasnight.game.logserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author 11
 * @date 2025/5/29 14:48
 */
@Component
public class LogStartManager implements SmartLifecycle {
    private Logger log = LoggerFactory.getLogger(getClass());

    private boolean running = false;

    @Override
    public void start() {
        try {
            File pidFile = new File("PID");
            log.info("输出 PID 文件，file=" + pidFile.getAbsolutePath());
            new ApplicationPid().write(pidFile);
            pidFile.deleteOnExit();
            //clearManager.clearLogFile();
        } catch (Exception ex) {
            String message = String.format("Cannot create pid file %s","PID");
            log.warn(message, ex);
        }
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
