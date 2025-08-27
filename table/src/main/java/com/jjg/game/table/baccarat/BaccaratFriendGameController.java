package com.jjg.game.table.baccarat;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.baccarat.gamephase.BaccaratFriendSettlementPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratSettlementPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratTableBetPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratTableWaitReadyPhase;

import java.util.LinkedHashSet;

/**
 * 百家乐好友房游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.BACCARAT, roomType = RoomType.BET_TEAM_UP_ROOM)
public class BaccaratFriendGameController extends BaccaratGameController{

    public BaccaratFriendGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> gamePhases = new LinkedHashSet<>();
        // 初始等待
        gamePhases.add(new BaccaratTableWaitReadyPhase(this));
        // 押注阶段
        gamePhases.add(new BaccaratTableBetPhase(this));
        // 进入结算(发牌、亮牌、补牌、结算对服务端来说只有一个阶段)
        gamePhases.add(new BaccaratFriendSettlementPhase(this));
        return gamePhases;
    }
}
