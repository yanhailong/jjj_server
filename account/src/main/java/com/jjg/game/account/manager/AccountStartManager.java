package com.jjg.game.account.manager;

import com.jjg.game.account.dao.PlayerIdDao;
import com.jjg.game.common.service.MarsCoreStartService;
import com.jjg.game.core.service.CoreStartService;
import com.jjg.game.core.service.LoginConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;

/**
 * @author 11
 * @date 2025/5/26 14:29
 */
@Component
public class AccountStartManager implements SmartLifecycle, ApplicationContextAware {
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;
    @Autowired
    private PlayerIdDao playerIdDao;
    @Autowired
    private LoginConfigService loginConfigService;

    private ApplicationContext context;

    private boolean running;

    @Override
    public void start() {
        marsCoreStartService.init(this.context, Collections.emptySet());
        coreStartService.init(this.context);

        this.loginConfigService.init();
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

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
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
