package com.jjg.game.table.luxurycarclub.gamephase;

import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.TableWaitReadyPhase;
import com.jjg.game.table.luxurycarclub.data.LuxuryCarClubGameDataVo;

/**
 * 豪车俱乐部等待阶段
 *
 * @author 2CL
 */
public class LuxuryCarClubReadyPhaseTable extends TableWaitReadyPhase<LuxuryCarClubGameDataVo> {

    public LuxuryCarClubReadyPhaseTable(AbstractPhaseGameController<Room_BetCfg, LuxuryCarClubGameDataVo> gameController) {
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
