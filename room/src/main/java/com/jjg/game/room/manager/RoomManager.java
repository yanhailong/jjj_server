package com.jjg.game.room.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.rpc.RpcCallSetting;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.rpc.HallRoomBridge;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.room.base.GameGm;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.friendroom.AbstractFriendRoomController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * 房间管理器
 *
 * @author 2CL
 */
@Component
public class RoomManager extends AbstractRoomManager implements GmListener, HallRoomBridge {

    public RoomManager() {
        super();
    }

    /**
     * 获取游戏中已经功能实现的游戏类型
     */
    public Set<EGameType> getGameAvailableTypes() {
        Set<Class<? extends AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>>> gameControllerClazz =
            getGameControllerClazz();
        // 已经实现具体的功能gameController游戏类型
        Set<EGameType> gameAvailableTypes = new HashSet<>();
        for (Class<? extends AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>>> controllerClazz : gameControllerClazz) {
            GameController gameAnnotateController = controllerClazz.getAnnotation(GameController.class);
            EGameType games = gameAnnotateController.gameType();
            gameAvailableTypes.add(games);
        }
        return gameAvailableTypes;
    }

    /**
     * 请求GM
     */
    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        if (gmOrders == null || gmOrders.length == 0) {
            return new CommonResult<>(Code.FAIL);
        }
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
            getGameControllerByPlayerId(playerController.playerId());
        if (gameController == null) {
            return new CommonResult<>(Code.FAIL);
        }
        Method[] methods = gameController.getClass().getMethods();
        if (methods.length == 0) {
            return new CommonResult<>(Code.NOT_FOUND);
        }
        try {
            for (Method method : methods) {
                if (!method.canAccess(gameController)) {
                    method.setAccessible(true);
                }
                int mode = method.getModifiers();
                if (Modifier.isAbstract(mode)) {
                    continue;
                }
                GameGm gameGmAnno = method.getDeclaredAnnotation(GameGm.class);
                if (gameGmAnno == null) {
                    continue;
                }
                if (gameGmAnno.cmd().equalsIgnoreCase(gmOrders[0])) {
                    // invoke gm
                    Object res = invokeGmMethod(gameController, method, playerController, gmOrders);
                    if (res == null) {
                        log.error("请求gm：{} 请求没有返回值 {}", gmOrders[0], gameController.getGameDataVo().roomLogInfo());
                        return new CommonResult<>(Code.FAIL);
                    }
                    if (res instanceof CommonResult<?> result) {
                        if (result.data instanceof String strData) {
                            return new CommonResult<>(result.code, strData);
                        } else {
                            return new CommonResult<>(result.code, JSON.toJSONString(result.data));
                        }
                    } else {
                        log.error("gm方法返回值不为 CommonResult类");
                        return new CommonResult<>(Code.FAIL);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return new CommonResult<>(Code.NOT_FOUND);
    }

    /**
     * invoke gm 方法
     */
    private Object invokeGmMethod(
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController,
        Method method, PlayerController playerController, String[] gmOrders) throws InvocationTargetException,
        IllegalAccessException {
        Class<?>[] types = method.getParameterTypes();
        if (types.length == 0) {
            return method.invoke(gameController);
        }
        Object[] params = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(PlayerController.class)) {
                params[i] = playerController;
            } else if (types[i].equals(String[].class)) {
                if (gmOrders.length > 1) {
                    String[] gmParams = new String[gmOrders.length - 1];
                    System.arraycopy(gmOrders, 1, gmParams, 0, gmOrders.length - 1);
                    params[i] = gmParams;
                } else {
                    params[i] = new String[]{};
                }
            } else {
                params[i] = null;
            }
        }
        return method.invoke(gameController, params);
    }

    /**
     * 先创建好空的好友房
     *
     * @param roomId 房间ID
     */
    @Override
    @RpcCallSetting(processorModKey = "#arg1")
    public void createFriendRoom(int roomCfgId, long roomId) {
        // 获取配置
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        if (warehouseCfg == null) {
            // 配置异常
            log.error("通过rpc调用好友房创建时异常，找不到WarehouseCfg配置， {}", roomCfgId);
            return;
        }
        Tuple2<Integer, Integer> tuples = SampleDataUtils.getRoomMaxLimit(warehouseCfg);
        try {
            AbstractRoomController<?, ?> roomController =
                initExistEmptyRoomByRoomId(warehouseCfg.getGameID(), roomCfgId, tuples.getT2(), roomId);
            if (roomController == null) {
                log.warn("通过cfgId: {} roomId: {} 初始化房间失败", roomCfgId, roomId);
            } else {
                log.warn("通过cfgId: {} roomId: {} 初始化房间成功", roomCfgId, roomId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 操作好友房
     *
     * @param roomId      房间ID
     * @param operateCode 操作码 1. 暂停 2. 重新开启 3. 解散
     */
    @Override
    @RpcCallSetting(processorModKey = "#arg1")
    public void operateFriendRoom(long playerId, long roomId, int operateCode) {
        if (operateCode < 1 || operateCode > 3) {
            return;
        }
        // 获取房间控制器
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController = getRoomControllerByRoomId(roomId);
        if (roomController == null) {
            log.debug("操作房间，但找不到指定的房间：{}", roomId);
            // TODO 如果是继续房间还需要查库和恢复房间的操作
            return;
        }
        if (roomController instanceof AbstractFriendRoomController<?, ?> friendRoomController) {
            // 房主
            long roomCreator = friendRoomController.getRoom().getCreator();
            if (roomCreator != playerId) {
                log.error("操作异常，玩家：{} 请求操作房间，但房间房主ID为：{}", playerId, roomCreator);
                return;
            }
            switch (operateCode) {
                case 1:
                    log.info("收到请求暂停房间：{} 的请求", roomId);
                    // 暂停房间
                    friendRoomController.pauseGame();
                    break;
                case 2:
                    log.info("收到请求继续房间：{} 的请求", roomId);
                    // 继续游戏
                    friendRoomController.tryContinueGame();
                    break;
                case 3:
                    log.info("收到请求结算房间：{} 的请求", roomId);
                    // 解散房间
                    friendRoomController.destroyOnNextRoundStart();
                    break;
            }
        }
    }

    @Override
    public FriendRoom getFriendRoomInfo(long roomId) {
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController = getRoomControllerByRoomId(roomId);
        if (roomController == null) {
            return null;
        }
        if (roomController.getRoom() instanceof FriendRoom friendRoom) {
            return friendRoom;
        }
        return null;
    }
}
