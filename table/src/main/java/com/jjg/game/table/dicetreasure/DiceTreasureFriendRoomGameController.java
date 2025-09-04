package com.jjg.game.table.dicetreasure;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseFriendRoomTableGameController;
import com.jjg.game.table.dicetreasure.data.DiceTreasureGameDataVo;
import com.jjg.game.table.dicetreasure.gamephase.DiceTreasureBetPhase;
import com.jjg.game.table.dicetreasure.gamephase.DiceTreasureSettlementPhase;
import com.jjg.game.table.dicetreasure.gamephase.DiceTreasureTableWaitReadyPhase;
import com.jjg.game.table.dicetreasure.message.DiceTreasureMessageBuilder;
import com.jjg.game.table.dicetreasure.message.NotifyDiceTreasureTableInfo;

import java.util.LinkedHashSet;

/**
 * 骰宝游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.DICE_TREASURE, roomType = RoomType.BET_TEAM_UP_ROOM)
public class DiceTreasureFriendRoomGameController extends BaseFriendRoomTableGameController<DiceTreasureGameDataVo> {

    public DiceTreasureFriendRoomGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        super.respRoomInitInfo(playerController);
        // 发送初始化数据
        NotifyDiceTreasureTableInfo animalsTableInfo =
            DiceTreasureMessageBuilder.notifyDiceTreasureTableInfo(playerController.playerId(), this, true);
        playerController.send(animalsTableInfo);
    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> roomPhases = new LinkedHashSet<>();
        roomPhases.add(new DiceTreasureTableWaitReadyPhase(this));
        roomPhases.add(new DiceTreasureBetPhase(this));
        roomPhases.add(new DiceTreasureSettlementPhase(this));
        return roomPhases;
    }

    @Override
    protected DiceTreasureGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new DiceTreasureGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public EGameType gameControlType() {
        return EGameType.DICE_TREASURE;
    }

}
