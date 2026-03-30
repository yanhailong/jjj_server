package com.jjg.game.table.baccarat;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.baccarat.gamephase.BaccaratSettlementPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratTableBetPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratTableWaitReadyPhase;
import com.jjg.game.table.baccarat.message.BaccaratMessageBuilder;
import com.jjg.game.table.baccarat.message.resp.RespBaccaratTableInfo;
import com.jjg.game.table.common.BaseFriendRoomTableGameController;

import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * 百家乐好友房游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.BACCARAT, roomType = RoomType.BET_TEAM_UP_ROOM)
public class BaccaratFriendGameController extends BaseFriendRoomTableGameController<BaccaratGameDataVo> {

    public BaccaratFriendGameController(AbstractRoomController<Room_BetCfg, FriendRoom> roomController) {
        super(roomController);
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> gamePhases = new LinkedHashSet<>();
        // 初始等待
        gamePhases.add(new BaccaratTableWaitReadyPhase(this));
        // 押注阶段
        gamePhases.add(new BaccaratTableBetPhase(this));
        // 进入结算(发牌、亮牌、补牌、结算对服务端来说只有一个阶段)
        gamePhases.add(new BaccaratSettlementPhase(this));
        return gamePhases;
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        super.respRoomInitInfo(playerController);
        EGamePhase eGamePhase = getCurrentGamePhase();
        // 如果在结算阶段需要从缓存中读取数据
        RespBaccaratTableInfo baccaratTableInfo =
            BaccaratMessageBuilder.buildRespBaccaratTableInfo(playerController.playerId(), this, gameDataVo, eGamePhase);
        // send
        playerController.send(Objects.requireNonNullElseGet(baccaratTableInfo,
            () -> new RespBaccaratTableInfo(Code.FAIL)));
    }

    @Override
    protected BaccaratGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new BaccaratGameDataVo(roomCfg);
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.BACCARAT;
    }
}
