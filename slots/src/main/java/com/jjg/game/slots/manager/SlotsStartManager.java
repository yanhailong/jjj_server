package com.jjg.game.slots.manager;

import com.jjg.game.common.service.MarsCoreStartService;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.service.CoreStartService;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressTestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;


/**
 * slots类游戏启动总线类
 * @author 11
 * @date 2025/6/10 16:37
 */
@Component
public class SlotsStartManager implements SmartLifecycle, ApplicationContextAware {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;
    @Autowired
    private SlotsSampleManager slotsSampleManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private DollarExpressTestManager testManager;
    //上下文
    private ApplicationContext context;

    private boolean running = false;

    @Override
    public void start() {
        //启动基础设施
        marsCoreStartService.init(this.context, Collections.emptySet());
        //启动core模块
        coreStartService.init(this.context);
        //加载excel配置
        this.slotsSampleManager.init();
        //初始化池子
        this.slotsPoolDao.initPool();
        //初始化所有游戏管理器
        initGameManager();
//        testManager.init();
        running = true;
    }

    @Override
    public void stop() {
        //关闭游戏管理器
        closeGameManager();
        //关闭core模块
        coreStartService.shutdown();
        //关闭基础设施
        marsCoreStartService.shutdown();

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

    /**
     * 初始化游戏管理器
     */
    private void initGameManager(){
        Map<String, AbstractSlotsGameManager> gameManagerMap = CommonUtil.getContext().getBeansOfType(AbstractSlotsGameManager.class);
        gameManagerMap.entrySet().forEach(en -> en.getValue().init());
    }

    /**
     * 关闭游戏管理器
     */
    private void closeGameManager(){
        Map<String, AbstractSlotsGameManager> gameManagerMap = CommonUtil.getContext().getBeansOfType(AbstractSlotsGameManager.class);
        gameManagerMap.entrySet().forEach(en -> en.getValue().shutdown());
    }
}
