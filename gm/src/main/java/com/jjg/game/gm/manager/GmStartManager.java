package com.jjg.game.gm.manager;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.service.MarsCoreStartService;
import com.jjg.game.core.service.CoreStartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * @author 11
 * @date 2025/5/29 14:39
 */
@Component
public class GmStartManager implements SmartLifecycle, ApplicationContextAware {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;
    @Autowired
    private NodeConfig nodeConfig;

    private ApplicationContext context;

    private boolean running = false;

    @Override
    public void start() {
        //为了安全，必须配置齐全才能启动服务
        if(nodeConfig.getWhiteIpList() == null || nodeConfig.getWhiteIdList().length < 1){
            throw new IllegalStateException("IP白名单检查失败，拒绝启动服务");
        }

        marsCoreStartService.init(this.context, Collections.emptySet());
        coreStartService.init(this.context);

        running = true;
    }

    @Override
    public void stop() {
        marsCoreStartService.shutdown();
        coreStartService.shutdown();

        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    /**
     * 检查api密钥
     */
    private void checkApiSecret(){

    }
}
