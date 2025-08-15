package com.jjg.game.table.dicetreasure.gamephase;

import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.dicetreasure.data.DiceTreasureGameDataVo;

/**
 * 骰宝
 *
 * @author 2CL
 */
public class DiceTreasureBetPhase extends BaseTableBetPhase<DiceTreasureGameDataVo> {

    public DiceTreasureBetPhase(AbstractPhaseGameController<Room_BetCfg, DiceTreasureGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public void phaseFinish() {

    }
}
