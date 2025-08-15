package com.jjg.game.table.luxurycarclub.gamephase;

import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.luxurycarclub.data.LuxuryCarClubGameDataVo;

/**
 * 豪车俱乐部
 *
 * @author 2CL
 */
public class LuxuryCarClubPhase extends BaseTableBetPhase<LuxuryCarClubGameDataVo> {

    public LuxuryCarClubPhase(AbstractPhaseGameController<Room_BetCfg, LuxuryCarClubGameDataVo> gameController) {
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


}
