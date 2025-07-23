package com.jjg.game.table.loongtigerwar.gamephase;

import com.jjg.game.table.common.gamephase.WaitReadyPhase;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;
import com.jjg.game.table.loongtigerwar.room.manager.LoongTigerWarRoomGameController;

/**
 * @author 2CL
 */
public class LoongTigerWarReadyPhase extends WaitReadyPhase<LoongTigerWarGameDataVo> {

    public LoongTigerWarReadyPhase(LoongTigerWarRoomGameController gameController) {
        super(gameController);
    }


    @Override
    public void phaseFinish() {

    }
}
