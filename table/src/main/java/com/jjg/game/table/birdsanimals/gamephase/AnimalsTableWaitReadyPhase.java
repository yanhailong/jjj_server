package com.jjg.game.table.birdsanimals.gamephase;

import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.birdsanimals.data.AnimalsGameDataVo;
import com.jjg.game.table.common.gamephase.TableWaitReadyPhase;

/**
 * 飞禽走兽等待阶段
 *
 * @author 2CL
 */
public class AnimalsTableWaitReadyPhase extends TableWaitReadyPhase<AnimalsGameDataVo> {

    public AnimalsTableWaitReadyPhase(AbstractPhaseGameController<Room_BetCfg, AnimalsGameDataVo> gameController) {
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
