package com.jjg.game.sim;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.ReqSimEnterGame;
import com.jjg.game.sim.pb.ReqSimExitGame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 模拟经营游戏消息处理器
 *
 * @author 11
 * @date 2026/5/15
 */
@Component
@MessageType(MessageConst.MessageTypeDef.SIM_GAME)
public class SimMessageHandler {
    @Autowired
    private SimManager simManager;

    /**
     * 进入游戏
     *
     * @param playerController
     * @param req
     */
    @Command(SimConstant.MsgBean.REQ_ENTER_GAME)
    public void reqEnterGame(PlayerController playerController, ReqSimEnterGame req) {
        simManager.onEnterGame(playerController);
    }

    /**
     * 退出
     *
     * @param playerController
     * @param req
     */
    @Command(SimConstant.MsgBean.REQ_EXIT_GAME)
    public void reqExitGame(PlayerController playerController, ReqSimExitGame req) {
        simManager.onExitGame(playerController.playerId(), ExitType.INITIATIVE);
    }
}
