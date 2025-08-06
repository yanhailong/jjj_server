package com.jjg.game.table.dicetreasure.gamephase;

import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.TableWaitReadyPhase;
import com.jjg.game.table.dicetreasure.data.DiceTreasureGameDataVo;

/**
 * 骰宝等待阶段
 *
 * @author 2CL
 */
public class DiceTreasureTableWaitReadyPhase extends TableWaitReadyPhase<DiceTreasureGameDataVo> {

    public DiceTreasureTableWaitReadyPhase(AbstractPhaseGameController<Room_BetCfg, DiceTreasureGameDataVo> gameController) {
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
