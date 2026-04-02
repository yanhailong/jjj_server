package com.jjg.game.table.russianlette;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseFriendRoomTableGameController;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;
import com.jjg.game.table.russianlette.gamephase.RussianLetteBetPhase;
import com.jjg.game.table.russianlette.gamephase.RussianLetteDrawPhase;
import com.jjg.game.table.russianlette.gamephase.RussianLetteSettlementPhase;
import com.jjg.game.table.russianlette.message.RussianLetteMessageBuilder;
import com.jjg.game.table.russianlette.message.resp.RespRussianLetteInfo;

import java.util.LinkedHashSet;

/**
 * 俄罗斯转盘游戏控制器
 *
 * @author lhc
 */
@GameController(gameType = EGameType.RUSSIAN_ROULETTE, roomType = RoomType.BET_TEAM_UP_ROOM)
public class RussianLetteFriendRoomGameController extends BaseFriendRoomTableGameController<RussianLetteGameDataVo> {

    public RussianLetteFriendRoomGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        super.respRoomInitInfo(playerController);
        // 发送初始化数据
//        NotifyRussianLetteTableInfo animalsTableInfo =
//            RussianLetteMessageBuilder.notifyAnimalsTableInfo(playerController.playerId(),this, true);
//        playerController.send(animalsTableInfo);

        RespRussianLetteInfo resp = RussianLetteMessageBuilder.buildRespRussianLetteInfo(
                playerController.playerId(), this);
        playerController.send(resp);
        // 更新玩家操作时间（心跳续约）
        TableGameDataVo tableGameDataVo = getGameDataVo();
        tableGameDataVo.updatePlayerOperateTime(playerController.playerId());
    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> roomPhases = new LinkedHashSet<>();
        roomPhases.add(new RussianLetteBetPhase(this));             // BET         stageTime[0]
        roomPhases.add(new RussianLetteDrawPhase(this));            // DRAW_ON     stageTime[1]
        roomPhases.add(new RussianLetteSettlementPhase(this));      // SETTLEMENT  stageTime[2]
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
