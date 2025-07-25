package com.jjg.game.table.baccarat.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.BetRobotCfg;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.baccarat.BaccaratGameController;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.baccarat.message.BaccaratMessageBuilder;
import com.jjg.game.table.baccarat.message.resp.NotifyBaccaratBetStart;
import com.jjg.game.table.betsample.sample.GameDataManager;
import com.jjg.game.table.betsample.sample.bean.BetAreaCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.common.message.req.ReqBet;
import com.jjg.game.table.common.message.bean.ReqBetBean;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.res.NotifyPlayerBet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 下注
 *
 * @author 2CL
 */
public class BaccaratTableBetPhase extends BaseTableBetPhase<BaccaratGameDataVo> {

    public BaccaratTableBetPhase(AbstractGameController<Room_BetCfg, BaccaratGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 向玩家通知场上数据,发送 BET_START 的阶段数据
        NotifyBaccaratBetStart baccaratTableInfo =
            BaccaratMessageBuilder.buildNotifyBaccaratBetStart(gameDataVo);
        broadcastMsgToRoom(baccaratTableInfo);
        // 通知所有观察者
        BaccaratMessageBuilder.notifyObserversOnPhaseChange((BaccaratGameController) gameController);
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
    public void phaseFinish() {
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
