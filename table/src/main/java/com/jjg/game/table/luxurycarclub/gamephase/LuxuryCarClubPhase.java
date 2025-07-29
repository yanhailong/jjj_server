package com.jjg.game.table.luxurycarclub.gamephase;

import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.luxurycarclub.data.LuxuryCarClubGameDataVo;

/**
 * 豪车俱乐部
 *
 * @author 2CL
 */
public class LuxuryCarClubPhase extends BaseTableBetPhase<LuxuryCarClubGameDataVo> {

    public LuxuryCarClubPhase(AbstractGameController<Room_BetCfg, LuxuryCarClubGameDataVo> gameController) {
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
