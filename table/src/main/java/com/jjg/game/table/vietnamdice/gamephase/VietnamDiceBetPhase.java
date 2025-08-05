package com.jjg.game.table.vietnamdice.gamephase;

import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.vietnamdice.data.VietnamDiceGameDataVo;

/**
 * 越南色碟
 *
 * @author 2CL
 */
public class VietnamDiceBetPhase extends BaseTableBetPhase<VietnamDiceGameDataVo> {

    public VietnamDiceBetPhase(AbstractGameController<Room_BetCfg, VietnamDiceGameDataVo> gameController) {
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
