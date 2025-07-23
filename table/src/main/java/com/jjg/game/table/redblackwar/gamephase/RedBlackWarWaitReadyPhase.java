package com.jjg.game.table.redblackwar.gamephase;

import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.table.common.gamephase.WaitReadyPhase;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;
import com.jjg.game.table.redblackwar.room.manager.RedBlackWarRoomGameController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author 2CL
 */
public class RedBlackWarWaitReadyPhase extends WaitReadyPhase<RedBlackWarGameDataVo> {

    public RedBlackWarWaitReadyPhase(RedBlackWarRoomGameController gameController) {
        super(gameController);
    }


    @Override
    public void phaseFinish() {
    }

}
