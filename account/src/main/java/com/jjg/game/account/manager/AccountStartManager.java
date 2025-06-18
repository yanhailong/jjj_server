package com.jjg.game.account.manager;

import com.jjg.game.account.dao.PlayerIdDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author 11
 * @date 2025/5/26 14:29
 */
@Component
public class AccountStartManager implements SmartLifecycle {
    protected Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private PlayerIdDao playerIdDao;

    private boolean running;

    @Override
    public void start() {
        this.playerIdDao.init();
        createPidFile();
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

    private void createPidFile(){
        try{
            File pidFile = new File("PID");
            log.info("输出 PID 文件，file=" + pidFile.getAbsolutePath());
            new ApplicationPid().write(pidFile);
            pidFile.deleteOnExit();
        }catch (Exception e){
            log.error("",e);
        }
    }
}
