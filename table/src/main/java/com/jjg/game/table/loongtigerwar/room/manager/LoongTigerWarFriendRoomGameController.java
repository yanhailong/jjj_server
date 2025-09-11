package com.jjg.game.table.loongtigerwar.room.manager;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseFriendRoomTableGameController;
import com.jjg.game.table.loongtigerwar.gamephase.LoongTigerWarBetPhase;
import com.jjg.game.table.loongtigerwar.gamephase.LoongTigerWarReadyPhaseTable;
import com.jjg.game.table.loongtigerwar.gamephase.LoongTigerWarSettlementPhase;
import com.jjg.game.table.loongtigerwar.message.LoongTigerWarMessageBuilder;
import com.jjg.game.table.loongtigerwar.message.resp.NotifyLoongTigerWarInfo;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;

import java.util.LinkedHashSet;

/**
 * 龙虎斗游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.LOONG_TIGER_WAR, roomType = RoomType.BET_TEAM_UP_ROOM)
public class LoongTigerWarFriendRoomGameController extends BaseFriendRoomTableGameController<LoongTigerWarGameDataVo> {

    public LoongTigerWarFriendRoomGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.LOONG_TIGER_WAR;
    }

    /**
     * @return 是否停止
     */
    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> gamePhases = new LinkedHashSet<>();
        gamePhases.add(new LoongTigerWarReadyPhaseTable(this));
        gamePhases.add(new LoongTigerWarBetPhase(this));
        gamePhases.add(new LoongTigerWarSettlementPhase(this));
        return gamePhases;
    }


    @Override
    protected LoongTigerWarGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new LoongTigerWarGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        super.respRoomInitInfo(playerController);
        //发送房间信息
        LoongTigerWarGameDataVo dataVo = getGameDataVo();
        NotifyLoongTigerWarInfo notifyLoongTigerWarInfo =
            LoongTigerWarMessageBuilder.buildInitInfo(this, playerController.playerId(), dataVo, getCurrentGamePhase());
        //发送给玩家
        broadcastToPlayers(
            RoomMessageBuilder.newBuilder().addPlayerId(playerController.playerId()).setData(notifyLoongTigerWarInfo));
    }

}
