package com.jjg.game.table.animals.gamephase;

import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.animals.data.AnimalsGameDataVo;
import com.jjg.game.table.common.gamephase.WaitReadyPhase;

/**
 * 飞禽走兽等待阶段
 *
 * @author 2CL
 */
public class AnimalsWaitReadyPhase extends WaitReadyPhase<AnimalsGameDataVo> {

    public AnimalsWaitReadyPhase(AbstractGameController<Room_BetCfg, AnimalsGameDataVo> gameController) {
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
