package com.jjg.game.sim.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimManager;
import com.jjg.game.sim.pb.req.*;
import com.jjg.game.sim.service.tick.SimGuestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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
     * 解锁建筑
     */
    @Command(SimConstant.MsgBean.REQ_UNLOCK_BUILDING)
    public void reqUnlockBuilding(PlayerController playerController, ReqUnlockBuilding req) {
        simManager.onUnlockBuilding(playerController, req.id);
    }

    /**
     * 升级建筑 (启动 CD)
     */
    @Command(SimConstant.MsgBean.REQ_UPGRADE_BUILDING)
    public void reqUpgradeBuilding(PlayerController playerController, ReqUpgradeBuilding req) {
        simManager.onUpgradeBuilding(playerController, req.id);
    }

    /**
     * 完成建筑升级
     */
    @Command(SimConstant.MsgBean.REQ_COMPLETE_BUILDING_UPGRADE)
    public void reqCompleteBuildingUpgrade(PlayerController playerController, ReqCompleteBuildingUpgrade req) {
        simManager.onCompleteBuildingUpgrade(playerController, req.id);
    }

    /**
     * 清除升级 CD
     */
    @Command(SimConstant.MsgBean.REQ_CLEAR_BUILDING_CD)
    public void reqClearBuildingCD(PlayerController playerController, ReqClearBuildingCD req) {
        Map<Integer, Long> costMap = new HashMap<>();
        if (req.costItems != null) {
            for (KVInfo kv : req.costItems) {
                costMap.put(kv.key, (long) kv.value);
            }
        }
        simManager.onClearBuildingCD(playerController, req.id, costMap);
    }

    /**
     * 招募雇员
     */
    @Command(SimConstant.MsgBean.REQ_RECRUIT_EMPLOYEE)
    public void reqRecruitEmployee(PlayerController playerController, ReqRecruitEmployee req) {
        simManager.onRecruitEmployee(playerController, req.employeeId);
    }

    /**
     * 升级雇员
     */
    @Command(SimConstant.MsgBean.REQ_UPGRADE_EMPLOYEE)
    public void reqUpgradeEmployee(PlayerController playerController, ReqUpgradeEmployee req) {
        simManager.onUpgradeEmployee(playerController, req.employeeId);
    }

    /**
     * 升星雇员
     */
    @Command(SimConstant.MsgBean.REQ_STAR_UP_EMPLOYEE)
    public void reqStarUpEmployee(PlayerController playerController, ReqStarUpEmployee req) {
        simManager.onStarUpEmployee(playerController, req.employeeId);
    }

    /**
     * 任命/更换主管
     */
    @Command(SimConstant.MsgBean.REQ_ASSIGN_SUPERVISOR)
    public void reqAssignSupervisor(PlayerController playerController, ReqAssignSupervisor req) {
        simManager.onAssignSupervisor(playerController, req.buildingId, req.employeeId);
    }

    @Command(SimConstant.MsgBean.REQ_SIM_GET_SKILLS)
    public void reqSlotsGetSkills(PlayerController playerController, ReqSimGetSkills req) {
        simManager.onLoadSlotsSkills(playerController);
    }

    @Command(SimConstant.MsgBean.REQ_SIM_UPGRADE_SKILL)
    public void reqSimUpgradeSkill(PlayerController playerController, ReqSimUpgradeSkill req) {
        simManager.onUpgradeSkill(playerController, req.gameType, req.skillId);
    }

    /**
     * 领取离线收益
     */
    @Command(SimConstant.MsgBean.REQ_CLAIM_OFFLINE_REWARD)
    public void reqClaimOfflineReward(PlayerController playerController, ReqClaimOfflineReward req) {
        simManager.onClaimOfflineReward(playerController, req.watchAd);
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
            } else if ("simExitGame".equalsIgnoreCase(gmOrders[0])) {
                reqExitGame(playerController, null);
            } else if ("printGuest".equalsIgnoreCase(gmOrders[0])) {
                SimPlayerContext context = simManager.getContext(playerController.playerId());
                context.printGuest();
            } else if ("printBuilding".equalsIgnoreCase(gmOrders[0])) {
                SimPlayerContext context = simManager.getContext(playerController.playerId());
                context.printBuilding();
            } else if ("unlockGuest".equalsIgnoreCase(gmOrders[0])) {
                SimPlayerContext context = simManager.getContext(playerController.playerId());
                int guestId = Integer.parseInt(gmOrders[1]);
                guestService.unlockGuest(context, guestId);
            } else if ("unlockBuilding".equalsIgnoreCase(gmOrders[0])) {
                ReqUnlockBuilding req = new ReqUnlockBuilding();
                req.id = Integer.parseInt(gmOrders[1]);
                reqUnlockBuilding(playerController, req);
            } else if ("upgradeBuilding".equalsIgnoreCase(gmOrders[0])) {
                ReqUpgradeBuilding req = new ReqUpgradeBuilding();
                req.id = Integer.parseInt(gmOrders[1]);
                reqUpgradeBuilding(playerController, req);
            } else if ("completeBuildingUpgrade".equalsIgnoreCase(gmOrders[0])) {
                ReqCompleteBuildingUpgrade req = new ReqCompleteBuildingUpgrade();
                req.id = Integer.parseInt(gmOrders[1]);
                reqCompleteBuildingUpgrade(playerController, req);
            } else if ("clearBuildingCD".equalsIgnoreCase(gmOrders[0])) {
                ReqClearBuildingCD req = new ReqClearBuildingCD();
                req.id = Integer.parseInt(gmOrders[1]);
                reqClearBuildingCD(playerController, req);
            } else if ("upgradeEmployee".equalsIgnoreCase(gmOrders[0])) {
                ReqUpgradeEmployee req = new ReqUpgradeEmployee();
                req.employeeId = Integer.parseInt(gmOrders[1]);
                reqUpgradeEmployee(playerController, req);
            } else if ("starUpEmployee".equalsIgnoreCase(gmOrders[0])) {
                ReqStarUpEmployee req = new ReqStarUpEmployee();
                req.employeeId = Integer.parseInt(gmOrders[1]);
                reqStarUpEmployee(playerController, req);
            } else if ("assignSupervisor".equalsIgnoreCase(gmOrders[0])) {
                ReqAssignSupervisor req = new ReqAssignSupervisor();
                req.buildingId = Integer.parseInt(gmOrders[1]);
                req.employeeId = Integer.parseInt(gmOrders[2]);
                reqAssignSupervisor(playerController, req);
            } else if ("recruitEmployee".equalsIgnoreCase(gmOrders[0])) {
                ReqRecruitEmployee req = new ReqRecruitEmployee();
                req.employeeId = Integer.parseInt(gmOrders[1]);
                reqRecruitEmployee(playerController, req);
            } else if ("settleOutput".equalsIgnoreCase(gmOrders[0])) {
                simManager.gmSettleOutput(playerController.playerId());
            } else if ("printPower".equalsIgnoreCase(gmOrders[0])) {
                simManager.gmPrintPower(playerController.playerId());
            } else if ("claimOffline".equalsIgnoreCase(gmOrders[0])) {
                boolean watchAd = gmOrders.length > 1 && "1".equals(gmOrders[1]);
                simManager.onClaimOfflineReward(playerController, watchAd);
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
