package com.jjg.game.table.animals.gamephase;

import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.animals.data.AnimalsGameDataVo;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;

/**
 * 飞禽走兽
 *
 * @author 2CL
 */
public class AnimalsBetPhase extends BaseTableBetPhase<AnimalsGameDataVo> {

    public AnimalsBetPhase(AbstractGameController<Room_BetCfg, AnimalsGameDataVo> gameController) {
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
