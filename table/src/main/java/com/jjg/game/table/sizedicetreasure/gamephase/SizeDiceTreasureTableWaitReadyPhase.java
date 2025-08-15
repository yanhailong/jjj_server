package com.jjg.game.table.sizedicetreasure.gamephase;

import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.TableWaitReadyPhase;
import com.jjg.game.table.sizedicetreasure.data.SizeDiceTreasureGameDataVo;

/**
 * 大小骰宝等待阶段
 *
 * @author 2CL
 */
public class SizeDiceTreasureTableWaitReadyPhase extends TableWaitReadyPhase<SizeDiceTreasureGameDataVo> {

    public SizeDiceTreasureTableWaitReadyPhase(AbstractPhaseGameController<Room_BetCfg, SizeDiceTreasureGameDataVo> gameController) {
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
