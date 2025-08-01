package com.jjg.game.table.redblackwar.gamephase;

import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;

/**
 * 下注
 *
 * @author 2CL
 */
public class RedBlackWarBetPhase extends BaseTableBetPhase<RedBlackWarGameDataVo> {

    public RedBlackWarBetPhase(AbstractGameController<Room_BetCfg, RedBlackWarGameDataVo> gameController) {
        super(gameController);
    }
}
