package com.jjg.game.table.redblackwar.gamephase;

import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.gamephase.TableWaitReadyPhase;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;

/**
 * @author 2CL
 */
public class RedBlackWarTableWaitReadyPhase extends TableWaitReadyPhase<RedBlackWarGameDataVo> {

    public RedBlackWarTableWaitReadyPhase(BaseTableGameController<RedBlackWarGameDataVo> gameController) {
        super(gameController);
    }


    @Override
    public void phaseFinish() {
    }

}
