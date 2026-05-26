package com.jjg.game.sim.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimManager;
import com.jjg.game.sim.pb.req.*;
import com.jjg.game.sim.service.SimGuestService;
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
    @Autowired
    private SimGuestService guestService;

    /**
     * 进入游戏
     */
    @Command(SimConstant.MsgBean.REQ_ENTER_GAME)
    public void reqEnterGame(PlayerController playerController, ReqSimEnterGame req) {
        simManager.onEnterGame(playerController);
    }

    /**
     * 退出
     */
    @Command(SimConstant.MsgBean.REQ_EXIT_GAME)
    public void reqExitGame(PlayerController playerController, ReqSimExitGame req) {
        simManager.onExitGame(playerController.playerId(), ExitType.INITIATIVE);
        playerController.setScene(null);
    }

    /**
     * 完成新手引导
     */
    @Command(SimConstant.MsgBean.REQ_FINISH_GUIDE)
    public void reqFinishGuide(PlayerController playerController, ReqFinishGuide req) {
        simManager.onFinishGuide(playerController.playerId());
    }

    /**
     * 请求同步游客位置
     */
    @Command(SimConstant.MsgBean.REQ_SYNC_GUEST_DEST)
    public void reqSyncGuestDest(PlayerController playerController, ReqSyncGuestLocation req) {
        simManager.onGuestLcation(playerController, req.guestId, req.buildingId, req.enter);
    }

    @Command(SimConstant.MsgBean.REQ_SIM_GET_SKILLS)
    public void reqSlotsGetSkills(PlayerController playerController, ReqSimGetSkills req) {
        simManager.onLoadSlotsSkills(playerController);
    }

    @Command(SimConstant.MsgBean.REQ_SIM_UPGRADE_SKILL)
    public void reqSimUpgradeSkill(PlayerController playerController, ReqSimUpgradeSkill req) {
        simManager.onUpgradeSkill(playerController, req.gameType, req.skillId);
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("simEnterGame".equalsIgnoreCase(gmOrders[0])) {
                ReqSimEnterGame req = new ReqSimEnterGame();
                reqEnterGame(playerController, req);
            } else if ("simFinishGuide".equalsIgnoreCase(gmOrders[0])) {
                reqFinishGuide(playerController, null);
            } else if ("syncGuestDest".equalsIgnoreCase(gmOrders[0])) {
                ReqSyncGuestLocation req = new ReqSyncGuestLocation();
                req.guestId = Integer.parseInt(gmOrders[1]);
                req.buildingId = Integer.parseInt(gmOrders[2]);
                req.enter = Boolean.parseBoolean(gmOrders[3]);
                reqSyncGuestDest(playerController, req);
            } else if ("simExitGame".equalsIgnoreCase(gmOrders[0])) {
                reqExitGame(playerController, null);
            } else if ("printGuest".equalsIgnoreCase(gmOrders[0])) {
                ((SimPlayerContext) playerController.getScene()).printGuest();
            } else if ("printBuilding".equalsIgnoreCase(gmOrders[0])) {
                ((SimPlayerContext) playerController.getScene()).printBuilding();
            } else if ("unlockGuest".equalsIgnoreCase(gmOrders[0])) {
                int guestId = Integer.parseInt(gmOrders[1]);
                SimPlayerContext ctx = (SimPlayerContext) playerController.getScene();
                guestService.unlockGuest(ctx, guestId);
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
