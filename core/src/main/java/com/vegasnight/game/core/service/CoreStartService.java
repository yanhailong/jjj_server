package com.vegasnight.game.core.service;

import com.vegasnight.game.core.manager.GameConfigManager;
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
    private GameConfigManager gameConfigManager;
    @Autowired
    private PlayerSessionService playerSessionService;


    /**
     * 启动时初始化
     * @param context
     */
    public void init(ApplicationContext context){
        gameConfigManager.init();
        playerSessionService.init();
    }

    /**
     * 关闭节点时
     */
    public void shutdown(){
        playerSessionService.shutdown();
    }
}
