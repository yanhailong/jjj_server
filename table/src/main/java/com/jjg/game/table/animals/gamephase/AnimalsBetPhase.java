package com.jjg.game.table.animals.gamephase;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.common.message.req.ReqBet;

public class AnimalsBetPhase extends BaseTableBetPhase {

    public AnimalsBetPhase(AbstractGameController gameController) {
        super(gameController);
    }


    @Override
    public void dealMsg(PlayerController playerController, AbstractMessage message) {

    }

    @Override
    public void phaseFinish() {

    }
}
