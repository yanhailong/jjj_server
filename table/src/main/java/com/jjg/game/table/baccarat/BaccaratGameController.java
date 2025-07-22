package com.jjg.game.table.baccarat;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.baccarat.gamephase.BaccaratSettlementPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratTableBetPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratWaitReadyPhase;
import com.jjg.game.table.common.BaseTableGameController;

import java.util.LinkedHashSet;

/**
 * 百家乐游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.BACCARAT)
public class BaccaratGameController extends BaseTableGameController<BaccaratGameDataVo> {

    public BaccaratGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.BACCARAT;
    }

    @Override
    public void autoRunGamePhase() {
        super.autoRunGamePhase();
        gameDataVo.setPhaseEndTime(System.currentTimeMillis() + currentGamePhase.getPhaseRunTime());
        gameDataVo.setPhaseRunTime(currentGamePhase.getPhaseRunTime());
    }

    /**
     * 百家乐的房间不会停止
     *
     * @return 是否停止
     */
    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> gamePhases = new LinkedHashSet<>();
        // 初始等待
        gamePhases.add(new BaccaratWaitReadyPhase(this));
        // 押注阶段
        gamePhases.add(new BaccaratTableBetPhase(this));
        // 进入结算(发牌、亮牌、补牌、结算对服务端来说只有一个阶段)
        gamePhases.add(new BaccaratSettlementPhase(this));
        return gamePhases;
    }

    @Override
    protected BaccaratGameDataVo copyRoomDataVo(GameDataVo<Room_BetCfg> roomData) {
        return new BaccaratGameDataVo(roomData.getRoomCfg());
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public void initGame() {
        // TODO 初始化机器人
        // 初始化启动流程

    }
}
