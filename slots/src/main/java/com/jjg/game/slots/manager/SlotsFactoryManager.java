package com.jjg.game.slots.manager;

import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomType;
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
    @Autowired
    private SlotsRoomManager slotsRoomManager;

    //所有的游戏管理器
    private Map<Integer,AbstractSlotsGameManager> slotsGameManagerMap = new HashMap<>();
    //所有的游戏管理器
    private Map<Integer,AbstractSlotsGameManager> slotsRoomGameManagerMap = new HashMap<>();

    /**
     * 工厂初始化
     */
    public void init(ApplicationContext context){
        //初始化池子
        this.slotsPoolDao.initPool();
        //初始化游戏管理器
        initGameManager(context);
        this.slotsRoomManager.init();
    }

    /**
     * 初始化游戏管理器
     */
    private void initGameManager(ApplicationContext context){
        Map<String, AbstractSlotsGameManager> gameManages = context.getBeansOfType(AbstractSlotsGameManager.class);
        gameManages.forEach((k,v) -> {
            v.init();
            int gameType = v.getGameType();
            if(v.getRoomType() == null){
                this.slotsGameManagerMap.put(gameType,v);
            }else if(v.getRoomType() == RoomType.SLOTS_TEAM_UP_ROOM){
                this.slotsRoomGameManagerMap.put(gameType,v);
            }else {
                throw new RuntimeException("roomType not support  " + v.getRoomType());
            }
        });
    }

    /**
     * 关闭游戏管理器
     */
    private void closeGameManager(){
        this.slotsGameManagerMap.forEach((k,v) -> v.shutdown());
    }

    public AbstractSlotsGameManager getGameManager(int gameType){
        return this.slotsGameManagerMap.get(gameType);
    }

    public AbstractSlotsGameManager getRoomGameManager(int gameType){
        return this.slotsRoomGameManagerMap.get(gameType);
    }

    public void clearPlayerEvent(long playerId){
        this.slotsGameManagerMap.forEach((k,v) -> v.clearPlayerEvent(playerId));
    }

    /**
     * 关闭工厂
     */
    public void shutdown(){
        closeGameManager();
        this.slotsRoomManager.shutDown();
    }
}
