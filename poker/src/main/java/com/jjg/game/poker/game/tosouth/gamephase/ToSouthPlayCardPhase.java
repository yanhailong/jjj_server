package com.jjg.game.poker.game.tosouth.gamephase;

import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BasePlayCardPhase;
import com.jjg.game.poker.game.common.gamephase.BaseWaitReadyPhase;
import com.jjg.game.poker.game.tosouth.room.ToSouthGameController;
import com.jjg.game.poker.game.tosouth.room.data.ToSouthGameDataVo;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToSouthPlayCardPhase extends BasePlayCardPhase<ToSouthGameDataVo> {
    private static final Logger log = LoggerFactory.getLogger(ToSouthPlayCardPhase.class);

    public ToSouthPlayCardPhase(AbstractPhaseGameController<Room_ChessCfg, ToSouthGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void playCardPhaseDoAction() {
        if (gameController instanceof ToSouthGameController controller) {
            if (gameDataVo.getPlayerSeatInfoList().isEmpty()) {
                controller.addPokerPhase(new BaseWaitReadyPhase<>(controller));
                return;
            }
            
            // 启动第一个玩家的回合
            PlayerSeatInfo firstPlayer = gameDataVo.getCurrentPlayerSeatInfo();
            if (firstPlayer != null) {
                // 广播回合开始
                controller.broadcastNextTurn(firstPlayer.getPlayerId(), gameDataVo.getCurRoundPassedPlayerSeats());
                // 添加操作定时器
                controller.addNextTimer(firstPlayer, 0);
            } else {
                log.error("PlayCardPhase start but no current player set!");
            }
        }
    }
}
