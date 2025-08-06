package com.jjg.game.table.sizedicetreasure;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.sizedicetreasure.data.SizeDiceTreasureGameDataVo;
import com.jjg.game.table.sizedicetreasure.gamephase.SizeDiceTreasureBetPhase;
import com.jjg.game.table.sizedicetreasure.gamephase.SizeDiceTreasureSettlementPhase;
import com.jjg.game.table.sizedicetreasure.gamephase.SizeDiceTreasureTableWaitReadyPhase;
import com.jjg.game.table.sizedicetreasure.message.NotifySizeDiceTreasureTableInfo;
import com.jjg.game.table.sizedicetreasure.message.SizeDiceTreasureMessageBuilder;

import java.util.LinkedHashSet;

/**
 * 大小骰宝游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.SIZE_DICE_TREASURE)
public class SizeDiceTreasureGameController extends BaseTableGameController<SizeDiceTreasureGameDataVo> {

    public SizeDiceTreasureGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        // 发送初始化数据
        NotifySizeDiceTreasureTableInfo animalsTableInfo =
            SizeDiceTreasureMessageBuilder.notifyAnimalsTableInfo(playerController.playerId(), this, true);
        playerController.send(animalsTableInfo);
    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> roomPhases = new LinkedHashSet<>();
        roomPhases.add(new SizeDiceTreasureTableWaitReadyPhase(this));
        roomPhases.add(new SizeDiceTreasureBetPhase(this));
        roomPhases.add(new SizeDiceTreasureSettlementPhase(this));
        return roomPhases;
    }

    @Override
    protected SizeDiceTreasureGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new SizeDiceTreasureGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public EGameType gameControlType() {
        return EGameType.SIZE_DICE_TREASURE;
    }
}
