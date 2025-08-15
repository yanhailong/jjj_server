package com.jjg.game.table.riveranimals.gamephase;

import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.TableWaitReadyPhase;
import com.jjg.game.table.riveranimals.data.RiverAnimalsGameDataVo;

/**
 * 鱼虾蟹等待阶段
 *
 * @author 2CL
 */
public class RiverAnimalsTableWaitReadyPhase extends TableWaitReadyPhase<RiverAnimalsGameDataVo> {

    public RiverAnimalsTableWaitReadyPhase(AbstractPhaseGameController<Room_BetCfg, RiverAnimalsGameDataVo> gameController) {
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
