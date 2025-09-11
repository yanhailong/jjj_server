package com.jjg.game.table.baccarat.gamephase;

import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BetAreaCfg;
import com.jjg.game.sampledata.bean.BetRobotCfg;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.baccarat.BaccaratGameController;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.baccarat.message.BaccaratMessageBuilder;
import com.jjg.game.table.baccarat.message.resp.NotifyBaccaratBetStart;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;

import java.util.HashMap;
import java.util.Map;

/**
 * 下注
 *
 * @author 2CL
 */
public class BaccaratTableBetPhase extends BaseTableBetPhase<BaccaratGameDataVo> {

    public BaccaratTableBetPhase(AbstractPhaseGameController<Room_BetCfg, BaccaratGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 向玩家通知场上数据,发送 BET_START 的阶段数据
        NotifyBaccaratBetStart baccaratTableInfo =
            BaccaratMessageBuilder.buildNotifyBaccaratBetStart(gameController, gameDataVo);
        broadcastMsgToRoom(baccaratTableInfo);
        // 通知所有观察者
        BaccaratMessageBuilder.notifyObserversOnPhaseChange((BaseTableGameController<BaccaratGameDataVo>) gameController);
    }

    @Override
    protected int robotRandomBetArea(BetRobotCfg betRobotCfg) {
        return super.robotRandomBetArea(betRobotCfg) % 10;
    }

    /**
     * 返回押注区域配置
     */
    @Override
    protected Map<Integer, BetAreaCfg> getBetAreaCfgMap() {
        return GameDataManager.getBetAreaCfgList()
            .stream()
            .filter(betRobotCfg -> betRobotCfg.getGameID() == gameController.gameControlType().getGameTypeId())
            .collect(HashMap::new, (map, cfg) -> map.put(cfg.getAreaID() % 10, cfg), HashMap::putAll);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
