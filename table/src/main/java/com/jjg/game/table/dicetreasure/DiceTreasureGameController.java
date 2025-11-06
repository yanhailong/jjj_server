package com.jjg.game.table.dicetreasure;

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
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.dicetreasure.data.DiceTreasureGameDataVo;
import com.jjg.game.table.dicetreasure.gamephase.DiceTreasureBetPhase;
import com.jjg.game.table.dicetreasure.gamephase.DiceTreasureSettlementPhase;
import com.jjg.game.table.dicetreasure.gamephase.DiceTreasureTableWaitReadyPhase;
import com.jjg.game.table.dicetreasure.message.DiceTreasureMessageBuilder;
import com.jjg.game.table.dicetreasure.message.NotifyDiceTreasureTableInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashSet;
import java.util.Map;

/**
 * 骰宝游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.DICE_TREASURE, roomType = RoomType.BET_ROOM)
public class DiceTreasureGameController extends BaseTableGameController<DiceTreasureGameDataVo> {

    public DiceTreasureGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
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

    @GameGm(cmd = "diceTreasure")
    public CommonResult<Map<Integer, Integer>> diceTreasureSettlement(String[] gmOrders) {
        if (gmOrders.length == 0) {
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        String[] split = StringUtils.split(gmOrders[0], ",");
        if (split.length != 3) {
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        gameDataVo.getGmResult().clear();
        for (String num : split) {
            gameDataVo.getGmResult().add(Integer.parseInt(num));
        }
        return new CommonResult<>(Code.SUCCESS);
    }
}
