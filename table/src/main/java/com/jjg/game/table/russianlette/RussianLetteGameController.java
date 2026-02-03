package com.jjg.game.table.russianlette;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;
import com.jjg.game.table.russianlette.gamephase.RussianLetteBetPhase;
import com.jjg.game.table.russianlette.gamephase.RussianLetteSettlementPhase;
import com.jjg.game.table.russianlette.gamephase.RussianLetteTableWaitReadyPhase;
import com.jjg.game.table.russianlette.message.NotifyRussianLetteTableInfo;
import com.jjg.game.table.russianlette.message.RussianLetteMessageBuilder;

import java.util.LinkedHashSet;

/**
 * 俄罗斯转盘游戏控制器
 *
 * @author lhc
 */
@GameController(gameType = EGameType.RUSSIAN_ROULETTE, roomType = RoomType.BET_ROOM)
public class RussianLetteGameController extends BaseTableGameController<RussianLetteGameDataVo> {

    public RussianLetteGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        // 发送初始化数据
        NotifyRussianLetteTableInfo animalsTableInfo =
            RussianLetteMessageBuilder.notifyAnimalsTableInfo(playerController.playerId(),this, true);
        playerController.send(animalsTableInfo);
    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> roomPhases = new LinkedHashSet<>();
        roomPhases.add(new RussianLetteTableWaitReadyPhase(this));
        roomPhases.add(new RussianLetteBetPhase(this));
        roomPhases.add(new RussianLetteSettlementPhase(this));
        return roomPhases;
    }

    @Override
    protected RussianLetteGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new RussianLetteGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public EGameType gameControlType() {
        return EGameType.RUSSIAN_ROULETTE;
    }
}
