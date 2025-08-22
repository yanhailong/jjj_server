package com.jjg.game.slots.manager;

import com.jjg.game.slots.dao.SlotsPoolDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/8/22 9:53
 */
@Component
public class SlotsFactoryManager {

    @Autowired
    private SlotsPoolDao slotsPoolDao;

    //所有的游戏管理器
    private Map<Integer,AbstractSlotsGameManager> slotsGameManagerMap = new HashMap<>();

    /**
     * 工厂初始化
     */
    public void init(ApplicationContext context){
        //初始化池子
        this.slotsPoolDao.initPool();
        //初始化游戏管理器
        initGameManager(context);
    }

    /**
     * 初始化游戏管理器
     */
    private void initGameManager(ApplicationContext context){
        Map<String, AbstractSlotsGameManager> gameManages = context.getBeansOfType(AbstractSlotsGameManager.class);
        gameManages.entrySet().forEach(en -> {
            AbstractSlotsGameManager manager = en.getValue();
            int gameType = manager.getGameType();
            this.slotsGameManagerMap.put(gameType,manager);
        });
    }

    /**
     * 关闭游戏管理器
     */
    private void closeGameManager(){
        this.slotsGameManagerMap.entrySet().forEach(en -> en.getValue().shutdown());
    }

    public AbstractSlotsGameManager getGameManager(int gameType){
        return this.slotsGameManagerMap.get(gameType);
    }

    /**
     * 关闭工厂
     */
    public void shutdown(){
        closeGameManager();
    }
}
