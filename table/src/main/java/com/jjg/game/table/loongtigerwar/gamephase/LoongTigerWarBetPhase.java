package com.jjg.game.table.loongtigerwar.gamephase;

import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;

/**
 * 下注
 *
 * @author 2CL
 */
public class LoongTigerWarBetPhase extends BaseTableBetPhase<LoongTigerWarGameDataVo> {

    public LoongTigerWarBetPhase(AbstractGameController<Room_BetCfg, LoongTigerWarGameDataVo> gameController) {
        super(gameController);
    }
}
