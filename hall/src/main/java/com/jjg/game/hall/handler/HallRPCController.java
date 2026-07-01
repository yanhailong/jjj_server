package com.jjg.game.hall.handler;

import com.jjg.game.alliance.bridge.ToAllianceBridge;
import com.jjg.game.alliance.service.AllianceCacheService;
import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.common.rpc.RpcCallSetting;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.handler.CoreRPCController;
import com.jjg.game.core.rpc.GmToHallBridge;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.hall.service.HallService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.data.SlotsSpinResult;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.data.VisitTrialSpinPermit;
import com.jjg.game.sim.manager.SimManager;
import com.jjg.game.sim.service.SimSkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author 11
 * @date 2026/1/19
 */
@Component
public class HallRPCController extends CoreRPCController implements GmToHallBridge, ToSimBridge, ToAllianceBridge {

    @Autowired
    private AccountDao accountDao;
    @Autowired
    private HallPlayerService playerService;
    @Autowired
    private HallService hallService;
    @Autowired
    private SimManager simManager;
    @Autowired
    private SimSkillService simSkillService;
    @Autowired
    private AllianceEventService allianceEventService;
    @Autowired
    private AllianceCacheService allianceCacheService;

    @Override
    public int playerBindPhone(long playerId, String phone, int type, boolean reward) {
        log.info("收到绑定或解绑手机请求 playerId = {},phone = {},type = {}", playerId, phone, type);
        int code = Code.SUCCESS;
        try {
            Player player = playerService.get(playerId);
            if (player == null) {
                log.warn("后台绑定或解绑手机时，未找到该玩家信息 playerId = {},phone = {},type = {}", playerId, phone, type);
                return Code.NOT_FOUND;
            }

            if (type == 1) {  //绑定
                return hallService.playerBindPhone(player, phone, reward).code;
            } else if (type == 2) {  //解绑
                CommonResult<Account> accountCommonResult = accountDao.removeThirdAccount(player, LoginType.PHONE);
                if (!accountCommonResult.success()) {
                    log.warn("绑定或解绑手机失败1 playerId = {},failCode = {}", player.getId(), accountCommonResult.code);
                    return accountCommonResult.code;
                }
                log.info("玩家解绑手机成功 playerId = {}", playerId, phone, type);
            } else {
                code = Code.FAIL;
                log.warn("不支持的绑定类型 playerId = {},phone = {},type = {}", playerId, phone, type);
            }
        } catch (Exception e) {
            log.error("", e);
            code = Code.EXCEPTION;
        }
        return code;
    }

    @Override
    public int afterVerifySmsSuccess(long playerId, String phone, int type) {
        try {
            log.info("大厅收到后台在短信验证成功后的消息 playerId = {},phone = {},type = {}", playerId, phone, type);
            VerCodeType verCodeType = VerCodeType.getType(type);
            if (verCodeType == null) {
                return Code.SUCCESS;
            }

            Player player = null;
            switch (verCodeType) {
                case SMS_BIND_PHONE:
                    player = playerService.get(playerId);
                    return hallService.playerBindPhone(player, phone, true).code;
                default:
                    return Code.SUCCESS;
            }
        } catch (Exception e) {
            log.error("", e);
            return Code.EXCEPTION;
        }
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public int deductResearchPoint(long playerId, Map<Integer, Integer> deductMap) {
        if (deductMap == null || deductMap.isEmpty()) {
            return Code.SUCCESS;
        }
        SimPlayerContext ctx = simManager.getContext(playerId);
        if (ctx == null) {
            log.warn("扣除研究点失败，未找到玩家 sim 数据 playerId={}", playerId);
            return Code.NOT_FOUND;
        }
        //先校验
        for (Map.Entry<Integer, Integer> en : deductMap.entrySet()) {
            if (ctx.getSimBaseData().findResearchPoint(en.getKey()) < en.getValue()) {
                log.warn("扣除研究点失败，研究点不足 playerId={},type={},need={}", playerId, en.getKey(), en.getValue());
                return Code.NOT_ENOUGH;
            }
        }
        //再扣
        for (Map.Entry<Integer, Integer> en : deductMap.entrySet()) {
            ctx.getSimBaseData().deductResearchPoint(en.getKey(), en.getValue());
        }
        return Code.SUCCESS;
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<SimSkillsData> addSkillById(long playerId, int gameType, int skillId) {
        ResearchSkillsCfg cfg = GameDataManager.getResearchSkillsCfg(skillId);
        if (cfg == null) {
            log.warn("添加技能失败，未找到技能配置 playerId={},skillId={}", playerId, skillId);
            return new CommonResult<>(Code.NOT_FOUND);
        }
        SimPlayerContext ctx = simManager.getContext(playerId);
        if (ctx == null) {
            log.warn("添加技能失败，未找到玩家 sim 数据 playerId={}", playerId);
            return new CommonResult<>(Code.NOT_FOUND);
        }

        SimSkillsData data = ctx.getSkillData(gameType);
        if (data == null) {
            log.warn("添加技能失败，该技能 playerId={}", playerId);
            return new CommonResult<>(Code.NOT_FOUND);
        }
        data.changeSkillLevel(cfg.getAttr(), cfg.getGrade());

        log.info("添加技能成功 playerId={},gameType={},skillId={},propId={},grade={}",
                playerId, gameType, skillId, cfg.getAttr(), cfg.getGrade());
        return new CommonResult<>(Code.SUCCESS, data);
    }

    @Override
    public CommonResult<SlotsSpinResult> onSlotsSpin(long playerId, int gameType, int winTimes, boolean changeNode,
                                                    SpinStatInfo statInfo, VisitTrialSpinPermit trialPermit) {
        return simManager.onSlotsSpin(playerId, gameType, winTimes, changeNode, statInfo, trialPermit);
    }

    @Override
    public CommonResult<VisitTrialSpinPermit> prepareVisitTrialSpin(long playerId, int gameType) {
        return simManager.prepareVisitTrialSpin(playerId, gameType);
    }

    @Override
    public CommonResult<Boolean> cancelVisitTrialSpin(long playerId, VisitTrialSpinPermit permit) {
        return simManager.cancelVisitTrialSpin(playerId, permit);
    }

    @Override
    public CommonResult<Map<Integer, Integer>> skillLevelUp(long playerId, int gameType, int skillId) {
        SimPlayerContext ctx = simManager.getContext(playerId);
        if (ctx == null) {
            log.warn("技能升级失败，未找到SimPlayerContext playerId={}", playerId);
            return new CommonResult<>(Code.NOT_FOUND);
        }
        return simSkillService.skillLevelUp(ctx, gameType, skillId);
    }

    // --------------------------- ToAllianceBridge (联盟跨节点入口) ---------------------------

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public void reportEarnGold(long playerId, int gameType, long gold) {
        allianceEventService.onEarnGold(playerId, gameType, gold);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public void reportAllianceEvent(long playerId, int conditionId, long param, long value) {
        allianceEventService.onEvent(playerId, conditionId, param, value);
    }

    @Override
    public long getPlayerAllianceId(long playerId) {
        return allianceCacheService.getAllianceId(playerId);
    }
}
