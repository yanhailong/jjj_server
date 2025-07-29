package com.jjg.game.table.luxurycarclub;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.luxurycarclub.data.LuxuryCarClubGameDataVo;
import com.jjg.game.table.luxurycarclub.gamephase.LuxuryCarClubPhase;
import com.jjg.game.table.luxurycarclub.gamephase.LuxuryCarClubReadyPhase;
import com.jjg.game.table.luxurycarclub.gamephase.LuxuryCarClubSettlementPhase;
import com.jjg.game.table.luxurycarclub.message.LuxuryCarClubMessageBuilder;
import com.jjg.game.table.luxurycarclub.message.NotifyLuxuryCarClubTableInfo;

import java.util.LinkedHashSet;

/**
 * 豪车俱乐部游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.LUXURY_CAR_CLUB)
public class LuxuryCarClubGameController extends BaseTableGameController<LuxuryCarClubGameDataVo> {

    public LuxuryCarClubGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        // 发送初始化数据
        NotifyLuxuryCarClubTableInfo animalsTableInfo =
            LuxuryCarClubMessageBuilder.notifyLuxuryCarClubTableInfo(this, true, playerController.playerId());
        playerController.send(animalsTableInfo);
    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> roomPhases = new LinkedHashSet<>();
        roomPhases.add(new LuxuryCarClubReadyPhase(this));
        roomPhases.add(new LuxuryCarClubPhase(this));
        roomPhases.add(new LuxuryCarClubSettlementPhase(this));
        return roomPhases;
    }

    @Override
    protected LuxuryCarClubGameDataVo copyRoomDataVo(GameDataVo<Room_BetCfg> roomData) {
        return new LuxuryCarClubGameDataVo(roomData.getRoomCfg());
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public EGameType gameControlType() {
        return EGameType.LUXURY_CAR_CLUB;
    }

    @Override
    public void initGame() {

    }
}
