package com.jjg.game.table.luxurycarclub;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.GameGm;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.BaseFriendRoomTableGameController;
import com.jjg.game.table.luxurycarclub.data.LuxuryCarClubGameDataVo;
import com.jjg.game.table.luxurycarclub.gamephase.LuxuryCarClubPhase;
import com.jjg.game.table.luxurycarclub.gamephase.LuxuryCarClubReadyPhaseTable;
import com.jjg.game.table.luxurycarclub.gamephase.LuxuryCarClubSettlementPhase;
import com.jjg.game.table.luxurycarclub.message.LuxuryCarClubMessageBuilder;
import com.jjg.game.table.luxurycarclub.message.NotifyLuxuryCarClubTableInfo;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * 豪车俱乐部游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.LUXURY_CAR_CLUB, roomType = RoomType.BET_TEAM_UP_ROOM)
public class LuxuryCarClubFriendRoomGameController extends BaseFriendRoomTableGameController<LuxuryCarClubGameDataVo> {

    public LuxuryCarClubFriendRoomGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        super.respRoomInitInfo(playerController);
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
        roomPhases.add(new LuxuryCarClubReadyPhaseTable(this));
        roomPhases.add(new LuxuryCarClubPhase(this));
        roomPhases.add(new LuxuryCarClubSettlementPhase(this));
        return roomPhases;
    }

    @Override
    protected LuxuryCarClubGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new LuxuryCarClubGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public EGameType gameControlType() {
        return EGameType.LUXURY_CAR_CLUB;
    }

    @GameGm(cmd = "settlement_simulate")
    public CommonResult<Map<Integer, Integer>> simulateSettlement(String[] gmOrders) {
        if (gmOrders.length == 0) {
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        int times = Integer.parseInt(gmOrders[0]);
        Map<Integer, Integer> res = new HashMap<>();
        for (int i = 0; i < times; i++) {
            WinPosWeightCfg winPosWeightCfg = LuxuryCarClubSettlementPhase.randomRewardCfgByWeight();
            res.put(winPosWeightCfg.getWinPosID(), res.getOrDefault(winPosWeightCfg.getWinPosID(), 0) + 1);
        }
        return new CommonResult<>(Code.SUCCESS, res);
    }
}
