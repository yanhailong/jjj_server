package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.room.controller.RoomController;
import com.jjg.game.room.dao.GoldRoomDao;
import com.jjg.game.room.dao.RoomDao;
import com.jjg.game.room.manager.AbstractGoldRoomManager;
import com.jjg.game.room.manager.AbstractRoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/25 15:45
 */
@Component
public class DollarRoomManager extends AbstractGoldRoomManager<RoomController,GoldRoomDao> {
    public DollarRoomManager(@Autowired GoldRoomDao roomDao) {
        super(RoomController.class,roomDao);
    }

    private int gameType = 100100;
    private int maxLimit = 100;

    @Autowired
    private CorePlayerService playerService;

    public void test(){
//        nodeCreateRoom(gameType,maxLimit, RoomType.YUEJU);

        Player player = playerService.get(1000009);
        player.setGameType(gameType);

        PlayerController playerController = new PlayerController(null, player);
//        joinRoom(playerController,gameType,436476);
//        exitRoom(playerController);
//        nodeCreateRoom(gameType,maxLimit,RoomType.YUEJU);
//        initRoom(maxLimit,RoomType.YUEJU);
        playerCreateRoom(playerController,gameType,maxLimit,RoomType.YUEJU);
    }
}
