package com.jjg.game.table.vietnamdice.gamephase;

import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.TableWaitReadyPhase;
import com.jjg.game.table.vietnamdice.data.VietnamDiceGameDataVo;

/**
 * 越南色碟等待阶段
 *
 * @author 2CL
 */
public class VietnamDiceTableWaitReadyPhase extends TableWaitReadyPhase<VietnamDiceGameDataVo> {

    public VietnamDiceTableWaitReadyPhase(AbstractGameController<Room_BetCfg, VietnamDiceGameDataVo> gameController) {
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
