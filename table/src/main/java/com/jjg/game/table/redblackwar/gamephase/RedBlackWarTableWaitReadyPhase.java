package com.jjg.game.table.redblackwar.gamephase;

import com.jjg.game.table.common.gamephase.TableWaitReadyPhase;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;
import com.jjg.game.table.redblackwar.room.manager.RedBlackWarRoomGameController;

/**
 * @author 2CL
 */
public class RedBlackWarTableWaitReadyPhase extends TableWaitReadyPhase<RedBlackWarGameDataVo> {

    public RedBlackWarTableWaitReadyPhase(RedBlackWarRoomGameController gameController) {
        super(gameController);
    }


    @Override
    public void phaseFinish() {
    }

}
