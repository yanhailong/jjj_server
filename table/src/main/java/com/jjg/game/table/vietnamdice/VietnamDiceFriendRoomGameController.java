package com.jjg.game.table.vietnamdice;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseFriendRoomTableGameController;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.vietnamdice.data.VietnamDiceGameDataVo;
import com.jjg.game.table.vietnamdice.gamephase.VietnamDiceBetPhase;
import com.jjg.game.table.vietnamdice.gamephase.VietnamDiceSettlementPhase;
import com.jjg.game.table.vietnamdice.gamephase.VietnamDiceTableWaitReadyPhase;
import com.jjg.game.table.vietnamdice.message.NotifyVietnamDiceTableInfo;
import com.jjg.game.table.vietnamdice.message.VietnamDiceMessageBuilder;

import java.util.LinkedHashSet;

/**
 * 越南色碟游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.VIETNAM_DICE, roomType = RoomType.BET_TEAM_UP_ROOM)
public class VietnamDiceFriendRoomGameController extends BaseFriendRoomTableGameController<VietnamDiceGameDataVo> {

    public VietnamDiceFriendRoomGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        // 发送初始化数据
        NotifyVietnamDiceTableInfo animalsTableInfo =
            VietnamDiceMessageBuilder.notifyVietnamDiceTableInfo(playerController.playerId(), this, true);
        playerController.send(animalsTableInfo);
    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> roomPhases = new LinkedHashSet<>();
        roomPhases.add(new VietnamDiceTableWaitReadyPhase(this));
        roomPhases.add(new VietnamDiceBetPhase(this));
        roomPhases.add(new VietnamDiceSettlementPhase(this));
        return roomPhases;
    }

    @Override
    protected VietnamDiceGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new VietnamDiceGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public EGameType gameControlType() {
        return EGameType.VIETNAM_DICE;
    }

}
