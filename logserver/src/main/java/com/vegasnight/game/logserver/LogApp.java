package com.vegasnight.game.logserver;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;

/**
 * @author 11
 * @date 2025/5/27 17:51
 */
@SpringBootApplication
@ComponentScan({"com.vegasnight.game"})
@EnableDubbo
public class LogApp implements SmartLifecycle{
    private Logger log = LoggerFactory.getLogger(getClass());

    private boolean running = false;

    public static void main(String[] args) {
        SpringApplication.run(LogApp.class, args);
    }

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
}
