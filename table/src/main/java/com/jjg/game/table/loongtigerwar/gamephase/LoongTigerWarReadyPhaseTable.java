package com.jjg.game.table.loongtigerwar.gamephase;

import com.jjg.game.table.common.gamephase.TableWaitReadyPhase;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;
import com.jjg.game.table.loongtigerwar.room.manager.LoongTigerWarRoomGameController;

/**
 * @author 2CL
 */
public class LoongTigerWarReadyPhaseTable extends TableWaitReadyPhase<LoongTigerWarGameDataVo> {

    public LoongTigerWarReadyPhaseTable(LoongTigerWarRoomGameController gameController) {
        super(gameController);
    }


    @Override
    public void phaseFinish() {

    }
}
