package com.jjg.game.sim.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimManager;
import com.jjg.game.sim.pb.req.*;
import com.jjg.game.sim.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

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
    @Autowired
    private SimBuildingService buildingService;
    @Autowired
    private SimEmployeeService employeeService;
    @Autowired
    private SimSkillService skillService;
    @Autowired
    private SimCasinoService casinoService;
    @Autowired
    private SimStatsService statsService;


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
        //退出 sim 界面不卸载 ctx, 场景后台继续运行直到玩家下线
        playerController.setScene(null);
    }

    /**
     * 完成新手引导
     */
    @Command(SimConstant.MsgBean.REQ_FINISH_GUIDE)
    public void reqFinishGuide(PlayerController playerController, ReqFinishGuide req) {
        simManager.onFinishGuide(playerController.playerId());
    }

    //--------------------------Casino相关 begin--------------------------

    /**
     * 开辟新场景
     */
    @Command(SimConstant.MsgBean.REQ_UNLOCK_CASINO)
    public void reqUnlockCasino(PlayerController playerController, ReqUnlockCasino req) {
        execute(playerController, ctx -> {
            casinoService.onUnlockCasino(ctx, req.casinoId);
        });
    }

    /**
     * 切换场景
     */
    @Command(SimConstant.MsgBean.REQ_SWITCH_CASINO)
    public void reqSwitchCasino(PlayerController playerController, ReqSwitchCasino req) {
        execute(playerController, ctx -> {
            casinoService.onSwitchCasino(ctx, req.casinoId);
        });
    }

    /**
     * 获取当前场景信息
     */
    @Command(SimConstant.MsgBean.REQ_CASINO_INFO)
    public void reqSimCasinoInfo(PlayerController playerController, ReqSimCasinoInfo req) {
        execute(playerController, ctx -> {
            casinoService.onCasinoInfo(ctx);
        });
    }

    //--------------------------建筑相关 begin--------------------------

    /**
     * 解锁建筑
     */
    @Command(SimConstant.MsgBean.REQ_UNLOCK_BUILDING)
    public void reqUnlockBuilding(PlayerController playerController, ReqUnlockBuilding req) {
        execute(playerController, ctx -> {
            buildingService.onUnlockBuilding(ctx, req.id);
        });
    }

    /**
     * 获取建筑信息
     */
    @Command(SimConstant.MsgBean.REQ_BUILDING_INFO)
    public void reqBuildingInfo(PlayerController playerController, ReqBuildingInfo req) {
        execute(playerController, ctx -> {
            buildingService.onBuildingInfo(ctx, req.id);
        });
    }

    /**
     * 升级建筑 (启动 CD)
     */
    @Command(SimConstant.MsgBean.REQ_UPGRADE_BUILDING)
    public void reqUpgradeBuilding(PlayerController playerController, ReqUpgradeBuilding req) {
        execute(playerController, ctx -> {
            buildingService.onUpgradeBuilding(ctx, req.id);
        });
    }

    /**
     * 完成建筑升级
     */
    @Command(SimConstant.MsgBean.REQ_COMPLETE_BUILDING_UPGRADE)
    public void reqCompleteBuildingUpgrade(PlayerController playerController, ReqCompleteBuildingUpgrade req) {
        execute(playerController, ctx -> {
            buildingService.onCompleteBuildingUpgrade(ctx, req.id);
        });
    }

    /**
     * 清除升级 CD
     */
    @Command(SimConstant.MsgBean.REQ_CLEAR_BUILDING_CD)
    public void reqClearBuildingCD(PlayerController playerController, ReqClearBuildingCD req) {
        execute(playerController, ctx -> {
            buildingService.onClearBuildingCD(ctx, req.id, req.costCount, req.watchAd);
        });
    }

    /**
     * 领取离线收益
     */
    @Command(SimConstant.MsgBean.REQ_CLAIM_OFFLINE_REWARD)
    public void reqClaimOfflineReward(PlayerController playerController, ReqClaimOfflineReward req) {
        execute(playerController, ctx -> {
            buildingService.onClaimOfflineReward(ctx, req.watchAd);
        });
    }


    //--------------------------雇员相关 begin--------------------------

    /**
     * 招募雇员
     */
    @Command(SimConstant.MsgBean.REQ_RECRUIT_EMPLOYEE)
    public void reqRecruitEmployee(PlayerController playerController, ReqRecruitEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onRecruitEmployee(ctx, req.count);
        });
    }

    /**
     * 升级雇员
     */
    @Command(SimConstant.MsgBean.REQ_UPGRADE_EMPLOYEE)
    public void reqUpgradeEmployee(PlayerController playerController, ReqUpgradeEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onUpgradeEmployee(ctx, req.employeeId);
        });
    }

    /**
     * 升星雇员
     */
    @Command(SimConstant.MsgBean.REQ_STAR_UP_EMPLOYEE)
    public void reqStarUpEmployee(PlayerController playerController, ReqStarUpEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onStarUpEmployee(ctx, req.employeeId);
        });
    }

    /**
     * 任命/更换主管
     */
    @Command(SimConstant.MsgBean.REQ_ASSIGN_SUPERVISOR)
    public void reqAssignSupervisor(PlayerController playerController, ReqAssignSupervisor req) {
        execute(playerController, ctx -> {
            employeeService.onAssignSupervisor(ctx, req.buildingId, req.employeeId);
        });
    }


    /**
     * 获取所有雇员
     */
    @Command(SimConstant.MsgBean.REQ_ALL_EMPLOYEE)
    public void reqAllEmployee(PlayerController playerController, ReqAllEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onAllEmployee(ctx);
        });
    }

    //--------------------------雇员相关 end--------------------------


    @Command(SimConstant.MsgBean.REQ_SIM_GET_SKILLS)
    public void reqSlotsGetSkills(PlayerController playerController, ReqSimGetSkills req) {
        execute(playerController, ctx -> {
            skillService.onLoadSlotsSkills(ctx, req.gameType);
        });
    }

    @Command(SimConstant.MsgBean.REQ_SIM_UPGRADE_SKILL)
    public void reqSimUpgradeSkill(PlayerController playerController, ReqSimUpgradeSkill req) {
        execute(playerController, ctx -> {
            skillService.onUpgradeSkill(ctx, req.gameType, req.skillId);
        });
    }

    //--------------------------游客相关 begin--------------------------

    /**
     * 生成购买游客 (点击购买后立即生成, 只预生成目的地)
     */
    @Command(SimConstant.MsgBean.REQ_GEN_PURCHASED_GUEST)
    public void reqGenPurchasedGuest(PlayerController playerController, ReqGenPurchasedGuest req) {
        execute(playerController, ctx -> {
            guestService.generatePurchasedGuest(ctx, req.guestId);
        });
    }

    /**
     * 领取购买游客奖励 (凭 uid 结算奖励)
     */
    @Command(SimConstant.MsgBean.REQ_PURCHASED_GUEST_REWARD)
    public void reqPurchasedGuestReward(PlayerController playerController, ReqPurchasedGuestReward req) {
        execute(playerController, ctx -> {
            guestService.claimPurchasedGuestReward(ctx, req.uid, req.index);
        });
    }

    /**
     * 获取所有游客
     */
    @Command(SimConstant.MsgBean.REQ_ALL_GUEST)
    public void reqAllGuest(PlayerController playerController, ReqAllGuest req) {
        execute(playerController, ctx -> {
            guestService.onAllGuest(ctx);
        });
    }

    /**
     * 招募游客
     */
    @Command(SimConstant.MsgBean.REQ_RECRUIT_GUEST)
    public void reqRecruitGuest(PlayerController playerController, ReqRecruitGuest req) {
        execute(playerController, ctx -> {
            guestService.onRecruitGuest(ctx, req.count);
        });
    }

    /**
     * 升星游客
     */
    @Command(SimConstant.MsgBean.REQ_STAR_UP_GUEST)
    public void reqStarUpGuest(PlayerController playerController, ReqStarUpGuest req) {
        execute(playerController, ctx -> {
            guestService.onStarUpGuest(ctx, req.guestId);
        });
    }

    //--------------------------游客相关 end--------------------------

    //--------------------------经营信息 begin--------------------------

    /**
     * 经营信息-运营数据
     */
    @Command(SimConstant.MsgBean.REQ_OPERATION_DATA)
    public void reqOperationData(PlayerController playerController, ReqOperationData req) {
        execute(playerController, ctx -> {
            statsService.onOperationData(ctx);
        });
    }

    /**
     * 经营信息-SPINE游戏数据 (指定游戏)
     */
    @Command(SimConstant.MsgBean.REQ_SLOT_STAT)
    public void reqSlotStat(PlayerController playerController, ReqSlotStat req) {
        execute(playerController, ctx -> {
            statsService.onSlotStat(ctx, req.gameType);
        });
    }

    //--------------------------经营信息 end--------------------------


    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("simFinishGuide".equalsIgnoreCase(gmOrders[0])) {
                reqFinishGuide(playerController, null);
            } else if ("printGuest".equalsIgnoreCase(gmOrders[0])) {
                SimPlayerContext context = simManager.getContext(playerController.playerId());
                context.printGuest();
            } else if ("printBuilding".equalsIgnoreCase(gmOrders[0])) {
                SimPlayerContext context = simManager.getContext(playerController.playerId());
                context.printBuilding();
            } else if ("unlockGuest".equalsIgnoreCase(gmOrders[0])) {
                int guestId = Integer.parseInt(gmOrders[1]);
                execute(playerController, ctx -> {
                    guestService.unlockGuest(ctx, guestId);
                });
            } else if ("unlockBuilding".equalsIgnoreCase(gmOrders[0])) {
                ReqUnlockBuilding req = new ReqUnlockBuilding();
                req.id = Integer.parseInt(gmOrders[1]);
                reqUnlockBuilding(playerController, req);
            } else if ("unlockAllBuilding".equalsIgnoreCase(gmOrders[0])) {
                SimPlayerContext ctx = simManager.getContext(playerController.playerId());
                for (BuildingAreaTableCfg cfg : GameDataManager.getBuildingAreaTableCfgList()) {
                    if (cfg.getRegionID() != ctx.getCurrentCasino().getCasinoId()) {
                        continue;
                    }
                    ReqUnlockBuilding req = new ReqUnlockBuilding();
                    req.id = cfg.getId();
                    reqUnlockBuilding(playerController, req);
                }
            } else if ("buildingInfo".equalsIgnoreCase(gmOrders[0])) {
                ReqBuildingInfo req = new ReqBuildingInfo();
                req.id = Integer.parseInt(gmOrders[1]);
                reqBuildingInfo(playerController, req);
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
                req.count = gmOrders.length > 1 ? Integer.parseInt(gmOrders[1]) : 1;
                reqRecruitEmployee(playerController, req);
            } else if ("claimOffline".equalsIgnoreCase(gmOrders[0])) {
                boolean watchAd = gmOrders.length > 1 && "1".equals(gmOrders[1]);
                ReqClaimOfflineReward req = new ReqClaimOfflineReward();
                req.watchAd = watchAd;
                reqClaimOfflineReward(playerController, req);
            } else if ("unlockCasino".equalsIgnoreCase(gmOrders[0])) {
                ReqUnlockCasino req = new ReqUnlockCasino();
                req.casinoId = Integer.parseInt(gmOrders[1]);
                reqUnlockCasino(playerController, req);
            } else if ("switchCasino".equalsIgnoreCase(gmOrders[0])) {
                ReqSwitchCasino req = new ReqSwitchCasino();
                req.casinoId = Integer.parseInt(gmOrders[1]);
                reqSwitchCasino(playerController, req);
            } else if ("addResearch".equalsIgnoreCase(gmOrders[0])) {
                int type = Integer.parseInt(gmOrders[1]);
                int num = Integer.parseInt(gmOrders[2]);
                SimPlayerContext context = simManager.getContext(playerController.playerId());
                context.getSimBaseData().addResearchPoint(type, num);
            } else if ("addPower".equalsIgnoreCase(gmOrders[0])) {
                int num = Integer.parseInt(gmOrders[1]);
                SimPlayerContext context = simManager.getContext(playerController.playerId());
                context.getSimBaseData().setPower(num + context.getSimBaseData().getPower());
            } else if ("caLevel".equalsIgnoreCase(gmOrders[0])) {
                reqSimCasinoInfo(playerController, null);
            } else if ("genGuest".equalsIgnoreCase(gmOrders[0])) {
                SimPlayerContext ctx = simManager.getContext(playerController.playerId());
                int num = Integer.parseInt(gmOrders[1]);
                if (num > 500 || num < 1) {
                    log.warn("单次生成游客数量区间在 1-500");
                    res.code = Code.FAIL;
                    return res;
                }
                //场景配置
                CasinoStatsSheetCfg casinoCfg = GameDataManager.getCasinoStatsSheetCfg(ctx.getCurrentCasino().getStatsId());
                if (casinoCfg == null) {
                    log.warn("生成游客失败，获取 CasinoStatsSheetCfg 配置未找到 playerId={},casinoId={}", ctx.playerId(), ctx.getCurrentCasino().getStatsId());
                    res.code = Code.FAIL;
                    return res;
                }
                guestService.batchGenerateGuest(ctx, num, casinoCfg, System.currentTimeMillis(), null);
            } else if ("specifyIdGenGuest".equalsIgnoreCase(gmOrders[0])) {
                int id = Integer.parseInt(gmOrders[1]);
                int num = Integer.parseInt(gmOrders[2]);
                if (num > 500 || num < 1) {
                    log.warn("单次生成游客数量区间在 1-500");
                    res.code = Code.FAIL;
                    return res;
                }

                SimPlayerContext context = simManager.getContext(playerController.playerId());
                guestService.batchGenerateSpecifyIdGuest(context, id, num);
            } else if ("specifyQualityGenGuest".equalsIgnoreCase(gmOrders[0])) {
                int quality = Integer.parseInt(gmOrders[1]);
                int num = Integer.parseInt(gmOrders[2]);
                if (num > 500 || num < 1) {
                    log.warn("单次生成游客数量区间在 1-500");
                    res.code = Code.FAIL;
                    return res;
                }

                SimPlayerContext context = simManager.getContext(playerController.playerId());
                guestService.batchGenerateSpecifyQualityGuest(context, quality, num);
            } else if ("genPurchasedGuest".equalsIgnoreCase(gmOrders[0])) {
                int guestId = Integer.parseInt(gmOrders[1]);
                execute(playerController, ctx -> {
                    guestService.generatePurchasedGuest(ctx, guestId);
                });
            } else if ("claimPurchasedGuest".equalsIgnoreCase(gmOrders[0])) {
                long uid = Long.parseLong(gmOrders[1]);
                int index = Integer.parseInt(gmOrders[2]);
                execute(playerController, ctx -> {
                    guestService.claimPurchasedGuestReward(ctx, uid, index);
                });
            } else if ("operationData".equalsIgnoreCase(gmOrders[0])) {
                reqOperationData(playerController, null);
            } else if ("slotStat".equalsIgnoreCase(gmOrders[0])) {
                ReqSlotStat req = new ReqSlotStat();
                req.gameType = gmOrders.length > 1 ? Integer.parseInt(gmOrders[1]) : 0;
                reqSlotStat(playerController, req);
            } else if ("casinoLevelUp".equalsIgnoreCase(gmOrders[0])) {
                int statsId = Integer.parseInt(gmOrders[1]);
                CasinoStatsSheetCfg cfg = GameDataManager.getCasinoStatsSheetCfg(statsId);
                if (cfg == null) {
                    res.code = Code.FAIL;
                    log.warn("未找到该配置 statsId={}", statsId);
                    return res;
                }
                SimPlayerContext ctx = simManager.getContext(playerController.playerId());
                ctx.getCurrentCasino().setStatsId(statsId);
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    public <T extends AbstractResponse> void execute(PlayerController pc, Consumer<SimPlayerContext> action) {
        SimPlayerContext ctx = simManager.getContext(pc.playerId());
        if (ctx == null) {
            log.warn("获取ctx为空 playerId={}", pc.playerId());
            return;
        }
        action.accept(ctx);
    }
}
