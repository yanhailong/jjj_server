package com.jjg.game.table.russianlette.gamephase;

import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;

/**
 * 俄罗斯转盘
 *
 * @author lhc
 */
public class RussianLetteBetPhase extends BaseTableBetPhase<RussianLetteGameDataVo> {

    public RussianLetteBetPhase(AbstractPhaseGameController<Room_BetCfg, RussianLetteGameDataVo> gameController) {
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
