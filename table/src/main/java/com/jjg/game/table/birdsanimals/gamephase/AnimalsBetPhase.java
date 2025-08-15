package com.jjg.game.table.birdsanimals.gamephase;

import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.birdsanimals.data.AnimalsGameDataVo;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;

/**
 * 飞禽走兽
 *
 * @author 2CL
 */
public class AnimalsBetPhase extends BaseTableBetPhase<AnimalsGameDataVo> {

    public AnimalsBetPhase(AbstractPhaseGameController<Room_BetCfg, AnimalsGameDataVo> gameController) {
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
