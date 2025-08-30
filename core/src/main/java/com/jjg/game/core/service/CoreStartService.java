package com.jjg.game.core.service;

import com.jjg.game.core.base.condition.ConditionCheckService;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.manager.SampleDataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * @author 11
 * @date 2025/5/27 10:08
 */
@Service
public class CoreStartService {

    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private SampleDataManager sampleDataManager;
    @Autowired
    private ConditionCheckService conditionCheckService;
    @Autowired
    private GameEventManager gameEventManager;

    /**
     * 启动时初始化
     *
     * @param context
     */
    public void init(ApplicationContext context) {
        playerSessionService.init();
        sampleDataManager.init();
        conditionCheckService.initConditionChecker();
        gameEventManager.initEventListener();
    }

    /**
     * 关闭节点时
     */
    public void shutdown() {
        playerSessionService.shutdown();
    }
}
