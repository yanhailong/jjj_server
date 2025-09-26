package com.jjg.game.poker.game.texas.gamephase;

import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.gamephase.BaseStartGamePhase;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

/**
 * @author lm
 * @date 2025/9/19 15:58
 */
public class TexasStartGamePhase extends BaseStartGamePhase<TexasGameDataVo> {

    public TexasStartGamePhase(AbstractPhaseGameController<Room_ChessCfg, TexasGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void nextPhase() {
        //设置当前游戏阶段为发牌
        if (gameController instanceof TexasGameController controller) {
            TexasPlayCardPhase gamePhase = new TexasPlayCardPhase(controller);
            controller.addPokerPhase(gamePhase);
            //通知场上信息
            PokerBuilder.buildNotifyPhaseChange(EGamePhase.PLAY_CART, -1);
        }
    }
}
