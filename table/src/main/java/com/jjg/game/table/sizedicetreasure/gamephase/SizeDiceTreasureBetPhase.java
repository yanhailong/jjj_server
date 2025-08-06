package com.jjg.game.table.sizedicetreasure.gamephase;

import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.sizedicetreasure.data.SizeDiceTreasureGameDataVo;

/**
 * 大小骰宝
 *
 * @author 2CL
 */
public class SizeDiceTreasureBetPhase extends BaseTableBetPhase<SizeDiceTreasureGameDataVo> {

    public SizeDiceTreasureBetPhase(AbstractPhaseGameController<Room_BetCfg, SizeDiceTreasureGameDataVo> gameController) {
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
