package com.jjg.game.sim;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.controller.SimGameController;
import com.jjg.game.sim.pb.req.ReqFinishGuide;
import com.jjg.game.sim.pb.req.ReqSimEnterGame;
import com.jjg.game.sim.pb.req.ReqSimExitGame;
import com.jjg.game.sim.pb.req.ReqSyncGuestLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class SimMessageHandler implements GmListener {
    private Logger log = LoggerFactory.getLogger(getClass());

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
        playerController.setScene(null);
    }

    /**
     * 完成新手引导
     *
     * @param playerController
     * @param req
     */
    @Command(SimConstant.MsgBean.REQ_FINISH_GUIDE)
    public void reqFinishGuide(PlayerController playerController, ReqFinishGuide req) {
        simManager.onFinishGuide(playerController.playerId());
    }

    /**
     * 请求同步游客位置
     *
     * @param playerController
     * @param req
     */
    @Command(SimConstant.MsgBean.REQ_SYNC_GUEST_DEST)
    public void reqSyncGuestDest(PlayerController playerController, ReqSyncGuestLocation req) {
        simManager.onGuestLcation(playerController, req.guestId, req.buildingId, req.enter);
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("simEnterGame".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到gm命令进入sim游戏 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                ReqSimEnterGame req = new ReqSimEnterGame();
                reqEnterGame(playerController, req);
            } else if ("simFinishGuide".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到gm命令完成新手引导 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                reqFinishGuide(playerController, null);
            } else if ("syncGuestDest".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到gm命令同步游客位置 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                ReqSyncGuestLocation req = new ReqSyncGuestLocation();
                req.guestId = Integer.parseInt(gmOrders[1]);
                req.buildingId = Integer.parseInt(gmOrders[2]);
                req.enter = Boolean.parseBoolean(gmOrders[3]);
                reqSyncGuestDest(playerController, req);
            } else if ("simExitGame".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到gm命令退出游戏 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                reqExitGame(playerController, null);
            } else if ("printGuest".equalsIgnoreCase(gmOrders[0])) {
                ((SimGameController) playerController.getScene()).printGuest();
            } else if ("unlockGuest".equalsIgnoreCase(gmOrders[0])) {
                int guestid = Integer.parseInt(gmOrders[1]);
                ((SimGameController) playerController.getScene()).unlockGuest(guestid);
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }
}
