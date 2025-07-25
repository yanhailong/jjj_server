package com.jjg.game.room.manager;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.bean.RoomCfg;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 房间管理器
 *
 * @author 2CL
 */
@Component
public class RoomManager extends AbstractRoomManager {

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

    public RoomManager() {
        super();
    }
}
