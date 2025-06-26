package com.jjg.game.room.manager;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.dao.AbstractGoldRoomDao;
import com.jjg.game.core.data.RoomType;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/25 18:22
 */
public class AbstractGoldRoomManager<C extends AbstractRoomController,D extends AbstractGoldRoomDao> extends AbstractRoomManager<C,D> {
    public AbstractGoldRoomManager(Class<C> roomControllerClazz,D roomDao) {
        super(roomControllerClazz,roomDao);
    }

    /**
     * 初始化房间，针对需要一直存在房间的游戏类型，并且至少存在一个房间
     */
    public void initRoom(int maxLimit, RoomType roomType){
        try{
            //先查询是否已有该游戏类型的的房间
            if(nodeConfig.getGameTypes() == null || nodeConfig.getGameTypes().length == 0){
                log.debug("该程序设置的支持游戏类型为空，初始化房间失败");
                return;
            }

            for(int gameType : nodeConfig.getGameTypes()){
                long count = roomDao.existRoomCount(gameType);
                if(count < 1){
                    //如果之前没有，就要创建一个房间
                    nodeCreateRoom(gameType,maxLimit,roomType);
                }else {
                    log.debug("该游戏已有初始房间存在，无需创建房间 gameType = {}",gameType);
                }
            }
        }catch (Exception e){
            log.error("",e);
        }
    }

    /**
     * 加入房间
     * @param playerController
     * @param gameType
     * @return
     */
    public void joinRoom(PlayerController playerController, int gameType) {
        try{
            int roomId = roomDao.getCanJoinRoomId(gameType);
            if(roomId < 1){
                log.debug("加入房间失败，获取到的房间id小于1，playerId = {},gameType = {}", roomId, gameType);
                return;
            }
            super.joinRoom(playerController,gameType,roomId);
        }catch (Exception e) {
            log.error("",e);
        }
    }

    @Override
    public void clearRoom() {
        try{
            log.debug("系统开始清除房间");

            if(nodeConfig.getGameTypes() == null || nodeConfig.getGameTypes().length == 0){
                log.debug("该程序设置的支持游戏类型为空，清除房间失败");
                return;
            }

            for(int gameType : nodeConfig.getGameTypes()){
                List<Object> allRoomIds = roomDao.getAllRoomIds(gameType);
                if(allRoomIds == null || allRoomIds.isEmpty()){
                    continue;
                }
                for(Object obj : allRoomIds){
                    int roomId = Integer.parseInt(obj.toString());
                    Long res = roomDao.removeRoom(gameType, roomId);
                    if(res != null && res > 0){
                        log.info("成功清除房间 gameType = {},roomId = {}", gameType, roomId);
                    }
                }
            }
        }catch (Exception e) {
            log.error("",e);
        }
    }
}
