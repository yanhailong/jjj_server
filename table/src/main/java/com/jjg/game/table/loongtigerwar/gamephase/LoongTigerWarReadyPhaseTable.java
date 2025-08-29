package com.jjg.game.table.loongtigerwar.gamephase;

import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.gamephase.TableWaitReadyPhase;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;

/**
 * @author 2CL
 */
public class LoongTigerWarReadyPhaseTable extends TableWaitReadyPhase<LoongTigerWarGameDataVo> {

    public LoongTigerWarReadyPhaseTable(BaseTableGameController<LoongTigerWarGameDataVo> gameController) {
        super(gameController);
    }


    @Override
    public void phaseFinish() {

    }
}
