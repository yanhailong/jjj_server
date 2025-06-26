package com.jjg.game.room.controller;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间控制器抽象类
 * @author 11
 * @date 2025/6/25 12:33
 */
public abstract class AbstractRoomController <P extends RoomPlayer,D extends AbstractRoomDao>{
    protected final Logger log = LoggerFactory.getLogger(getClass());

    //房间对象
    protected Room room;
    //当前房间的玩家
    protected Map<Long, PlayerController> playerControllers = new ConcurrentHashMap<>();

    protected Class<P> roomPlayerClazz;
    protected D roomDao;

    public AbstractRoomController(Class<P> roomPlayerClazz) {
        this.roomPlayerClazz = roomPlayerClazz;
    }

    /**
     * 玩家加入房间
     * @param playerController
     */
    public CommonResult<Room> joinRoom(PlayerController playerController){
        CommonResult<Room> result = new CommonResult<>(Code.SUCCESS);
        try{
            //检查玩家在不在房间
            RoomPlayer roomPlayer = room.getPlayer(playerController.playerId());
            //如果不在房间
            if(roomPlayer == null){
                CommonResult<Room> doResult = roomDao.doSave(playerController.player.getGameType(),this.room.getId(), r -> {
                    try{
                        //先检查该玩家是否已经在该房间中
                        boolean exist = r.hasPlayer(playerController.playerId());
                        if(exist){
                            log.debug("玩家已在房间中 gameType = {},roomId = {},playerId = {}", playerController.player.getGameType(), this.room.getId(),playerController.playerId());
                            return true;
                        }

                        if(!r.canEnter()){
                            log.debug("该房间无法进入 gameType = {},roomId = {},playerId = {}", playerController.player.getGameType(), this.room.getId(),playerController.playerId());
                            return false;
                        }

                        //如果之前不在房间，则按座位排座
                        for(int i=0;i<r.getMaxLimit();i++){
                            boolean flag = r.setHasPlayer(i);
                            if(!flag){
                                RoomPlayer tmpRoomPlayer = roomDao.createRoomPlayer(playerController.playerId());
                                tmpRoomPlayer.setSit(i);
                                r.addPlayer(tmpRoomPlayer);
                                return true;
                            }
                        }
                    }catch (Exception e) {
                        log.error("",e);
                    }
                    return false;
                });

                if(!doResult.success()){
                    log.warn("加入房间失败 gameType = {},roomId = {},playerId = {}", playerController.player.getGameType(), this.room.getId(),playerController.playerId());
                    result.code = Code.FAIL;
                    return result;
                }
                this.room = doResult.data;
            }else {
                log.debug("玩家已经在房间中 roomId = {},playerId = {}", room.getId(), playerController.playerId());
            }

            playerControllers.put(playerController.playerId(), playerController);
            playerController.setScene(this);

            //TODO 广播新玩家进入
            log.debug("进入房间成功，但是还没有广播新玩家进入 roomId = {},playerId = {}", room.getId(), playerController.playerId());

            result.data = room;
            return result;
        }catch (Exception e){
            log.error("",e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 玩家退出房间
     * @param playerController
     * @return
     */
    public CommonResult<Room> exitRoom(PlayerController playerController){
        CommonResult<Room> result = new CommonResult<>(Code.SUCCESS);
        try{
            //从room中移除
            RoomPlayer roomPlayer = room.exit(playerController.playerId());
            if(roomPlayer == null){
                log.debug("将玩家从房间中移除失败 gameType = {},roomId = {},playerId = {}", room.getGameType(), room.getId(),playerController.playerId());
                result.code = Code.FAIL;
                return result;
            }

            //TODO 广播新玩家退出
            log.debug("退出房间成功，但是还没有广播玩家退出 roomId = {},playerId = {}", room.getId(), playerController.playerId());

            result.data = room;
            roomDao.saveRoom(room);
        }catch (Exception e){
            log.error("",e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }


    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Map<Long, PlayerController> getPlayerControllers() {
        return playerControllers;
    }

    public void setPlayerControllers(Map<Long, PlayerController> playerControllers) {
        this.playerControllers = playerControllers;
    }

    public void addPlayerController(PlayerController playerController){
        playerControllers.put(playerController.playerId(), playerController);
    }

    public D getRoomDao() {
        return roomDao;
    }

    public void setRoomDao(D roomDao) {
        this.roomDao = roomDao;
    }
}
