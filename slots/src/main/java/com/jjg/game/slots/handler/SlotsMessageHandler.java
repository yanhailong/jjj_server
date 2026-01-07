package com.jjg.game.slots.handler;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.ChooseWareListener;
import com.jjg.game.core.pb.ReqChooseWare;
import com.jjg.game.core.pb.ResChooseWare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SlotsMessageHandler implements ChooseWareListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void onChooseWare(PlayerController playerController, ReqChooseWare req) {
        ResChooseWare res = new ResChooseWare(Code.SUCCESS);
        if (req.gameType == playerController.getPlayer().getGameType() && req.wareId == playerController.getPlayer().getRoomCfgId()) {
            playerController.send(res);
            return;
        }

        log.warn("玩家选择场次信息错误 playerId = {},playerGameType = {},playerWareId = {},reqGameType = {},reqRoomCfgId = {}"
                , playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(),
                req.gameType, req.wareId);
        res.code = Code.FAIL;
        playerController.send(res);
    }
}
