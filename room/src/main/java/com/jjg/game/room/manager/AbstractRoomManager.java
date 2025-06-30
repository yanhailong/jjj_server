package com.jjg.game.room.manager;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.RoomType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 11
 * @date 2025/6/25 10:19
 */
public abstract class AbstractRoomManager<C extends AbstractRoomController,D extends AbstractRoomDao> {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected NodeManager nodeManager;
    @Autowired
    protected ClusterSystem clusterSystem;
    @Autowired
    protected NodeConfig nodeConfig;
    @Autowired
    protected CorePlayerService playerService;

    protected AbstractRoomDao roomDao;

    //所有的房间控制器  gameType -> roomId - > RoomController
    protected Map<Integer,Map<Integer, C>> roomControllerMap = new ConcurrentHashMap<>();

    protected Class<C> roomControllerClazz;

    public AbstractRoomManager(Class<C> roomControllerClazz,D roomDao) {
        this.roomControllerClazz = roomControllerClazz;
        this.roomDao = roomDao;
    }

    public void init(){
//        Map<String, AbstractRoomDao> roomManagerMap = CommonUtil.getContext().getBeansOfType(AbstractRoomDao.class);
//        for(Map.Entry<String, AbstractRoomDao> en : roomManagerMap.entrySet()){
//            this.roomDao = en.getValue();
//            break;
//        }
//        if(this.roomDao == null){
//            throw new RuntimeException("roomDao 不能未空");
//        }
    }

    /**
     * 系统创建房间
     * @param gameType
     */
    public C nodeCreateRoom(int gameType, int wareId, int maxLimit,RoomType roomType) {
        try{
            Room room = roomDao.nodeCreate(gameType,wareId,maxLimit, this.nodeManager.getNodePath(), roomType);
            if(room == null){
                log.warn("创建房间失败 gameType = {},wareId = {},roomType = {}", gameType,wareId, roomType);
                return null;
            }

            C roomController = createRoomController();

            roomController.setRoom(room);
            addRoomController(gameType,room.getId(), roomController);

            log.debug("系统创建房间成功 gameType = {},wareId = {},roomType = {},maxLimit = {}", gameType, wareId,roomType,maxLimit);
            return roomController;
        }catch (Exception e) {
            log.error("",e);
        }
        return null;
    }

    /**
     * 玩家创建房间
     * @param gameType
     * @param roomType
     * @return
     */
    public C playerCreateRoom(PlayerController playerController, int gameType, int maxLimit,RoomType roomType) {
        try{
            Room room = roomDao.createRoom(playerController.playerId(),gameType, maxLimit, this.nodeManager.getNodePath(), roomType);
            if(room == null){
                log.warn("创建房间失败 playerId = {},gameType = {},roomType = {}", playerController.playerId(),gameType, roomType);
                return null;
            }
            C roomController = createRoomController();

            roomController.setRoom(room);
            roomController.addPlayerController(playerController);
            addRoomController(gameType,room.getId(), roomController);

            log.debug("玩家创建房间成功 playerId = {},gameType = {},roomType = {},maxLimit = {}", playerController.playerId(),gameType, roomType,maxLimit);
            return roomController;
        }catch (Exception e) {
            log.error("",e);
        }
        return null;
    }

    /**
     * player加入房间
     * @param playerController
     * @param roomId
     * @return
     */
    public int joinRoom(PlayerController playerController,int gameType,int roomId) {
        try{
            if(roomId < 1){
                log.debug("roomId不能小于,加入房间失败 gameType = {},roomId = {} ,playerId = {}", gameType, roomId,playerController.playerId());
                return Code.FAIL;
            }

            C roomController = getRoomController(gameType,roomId);
            if (roomController == null) {
                //从redis获取房间数据
                Room room = roomDao.getRoom(gameType, roomId);
                if (room == null) {
                    log.warn("加入房间失败，该房间不存在 gameType = {},roomId = {},playerId = {}", gameType, roomId,playerController.playerId());
                    return Code.FAIL;
                }

                //如果该房间所在节点不是本节点，就要切换节点
                if(!room.getPath().equals(this.nodeManager.getNodePath())){
                    MarsNode node = clusterSystem.getNode(room.getPath());
                    if(node == null){
                        log.warn("加入房间成功，开始切换节点 gameType = {},roomId = {},playerId = {},toRoomPath = {}", gameType, roomId,playerController.playerId(), room.getPath());
                        return Code.FAIL;
                    }
                    clusterSystem.switchNode(playerController.session,node);
                    return Code.SUCCESS;
                }

                //如果是本节点，先创建roomConrtoller
                roomController = createRoomController();
                addRoomController(gameType,roomId, roomController);
                roomController.setRoom(room);
            }

            //roomController不为空，那么room就是在本节点
            CommonResult<Room> addResult = roomController.joinRoom(playerController);
            if(!addResult.success()){
                return Code.FAIL;
            }
            roomController.setRoom(addResult.data);
            playerController.player = playerService.doSave(playerController.playerId(), p -> p.setRoomId(roomId));
            log.debug("玩家加入房间成功 gameType = {},roomId = {}, playerId = {}", gameType, roomId,playerController.playerId());
            return Code.SUCCESS;
        }catch (Exception e) {
            log.error("",e);
        }
        return Code.FAIL;
    }

    /**
     * 玩家退出房间
     * @param playerController
     */
    protected int exitRoom(PlayerController playerController) {
        try{
            if(playerController.roomId() < 1){
                log.debug("roomId不能小于,退出房间失败 gameType = {},playerId = {}", playerController.player.getGameType(), playerController.playerId());
                return Code.FAIL;
            }

            C roomController = getRoomController(playerController.player.getGameType(),playerController.roomId());
            if (roomController == null) {
                boolean remove = roomDao.removePlayer(playerController.player.getGameType(),playerController.roomId(),playerController.playerId());
                if(remove){
                    log.debug("强制离开房间成功, gameType = {},roomId = {},playerId = {}", playerController.player.getGameType(),playerController.roomId(),playerController.playerId());
                    return Code.SUCCESS;
                }
                log.warn("退出房间失败，该房间不存在 gameType = {},roomId = {},playerId = {}", playerController.player.getGameType(), playerController.roomId(),playerController.playerId());
                return Code.FAIL;
            }

            CommonResult<Room> roomResult = roomController.exitRoom(playerController);
            if(!roomResult.success()){
                return roomResult.code;
            }
            return Code.SUCCESS;
        }catch (Exception e) {
            log.error("",e);
        }
        return Code.FAIL;
    }

    /**
     * 清除房间
     */
    public void clearRoom(){

    }

    /**
     * 获取 RoomController
     * @param gameType
     * @param roomId
     * @return
     */
    protected C getRoomController(int gameType,int roomId) {
        Map<Integer, C> tempMap = this.roomControllerMap.get(gameType);
        if(tempMap == null || tempMap.isEmpty()){
            return null;
        }

        return tempMap.get(roomId);
    }

    protected void addRoomController(int gameType,int roomId,C roomController) {
        this.roomControllerMap.computeIfAbsent(gameType, k -> new ConcurrentHashMap<>()).computeIfAbsent(roomId, k -> roomController);
    }

    private C createRoomController() throws Exception{
        Constructor<C> controllerConstructor = this.roomControllerClazz.getConstructor();
        C roomController = controllerConstructor.newInstance();
        roomController.setRoomDao(roomDao);
        return roomController;
    }
}
