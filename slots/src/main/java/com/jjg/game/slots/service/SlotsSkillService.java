package com.jjg.game.slots.service;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PropInfo;
import com.jjg.game.core.utils.PropUtil;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.service.AbstractSkillService;
import com.jjg.game.sim.service.SimNodeService;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author 11
 * @date 2026/5/25
 */
@Service
public class SlotsSkillService extends AbstractSkillService {

    @ClusterRpcReference()
    private ToSimBridge toSimBridge;
    @Autowired
    private SimNodeService simNodeService;

    public int addSkillById(SlotsPlayerGameData playerGameData, int skillId) {
        //获取节点
        ClusterClient client = simNodeService.getSimClusterClient(playerGameData.getPlayerId());
        if (client == null) {
            log.warn("添加技能失败，未找到sim 节点 playerId = {}", playerGameData.getPlayerId());
            return Code.FAIL;
        }

        GameRpcContext.getContext().withReqParameterBuilder(RpcReqParameterBuilder.create().addClusterClient(client).setTryMillisPerClient(1000));

        CommonResult<SimSkillsData> commonResult = toSimBridge.addSkillById(playerGameData.getPlayerId(), playerGameData.getGameType(), skillId);
        if (commonResult == null || !commonResult.success()) {
            log.warn("添加技能失败 playerId = {},code={}", playerGameData.getPlayerId(), commonResult == null ? "null" : commonResult.code);
            return Code.FAIL;
        }
        if (commonResult.data == null) {
            log.warn("添加技能失败，返回数据为空 playerId = {}", playerGameData.getPlayerId());
            return Code.FAIL;
        }
        playerGameData.setSimSkillsData(commonResult.data);
        return Code.SUCCESS;
    }

    /**
     * 判断 betValue 是否为玩家技能解锁的下注额
     *
     * @param data
     * @param betValue
     * @return
     */
    public boolean isSkillBet(SimSkillsData data, long betValue) {
        if (data == null || data.getStakeList() == null) {
            return false;
        }
        return data.getStakeList().contains(betValue);
    }

    /**
     * 将玩家技能 specialMode (libType -> weightDelta) 累加到 typeProp 权重上，返回修改后的克隆
     */
    protected PropInfo applySpecialModeSkillBonus(PropInfo propInfo, List<ResearchSkillsCfg> skills) {
        //将玩家技能中所有影响libType权重的加成合并
        Map<Integer, Integer> deltaMap = new HashMap<>();
        for (ResearchSkillsCfg cfg : skills) {
            Map<Integer, Integer> specialMode = cfg.getSpecialMode();
            if (specialMode == null || specialMode.isEmpty()) {
                continue;
            }
            specialMode.forEach((key, value) -> deltaMap.merge(key, value, Integer::sum));
        }
        return PropUtil.applyPropInfoDelta(propInfo, deltaMap);
    }

    /**
     * 将玩家技能 winRate + specialModeProbUp 中匹配 libType 的 (sectionIdx -> delta) 累加到 section 权重上，返回修改后的克隆
     */
    protected PropInfo applySectionSkillBonus(PropInfo propInfo, int libType, List<ResearchSkillsCfg> skillCfgList) {
        //将玩家技能中所有匹配libType的 section 权重加成合并
        Map<Integer, Integer> deltaMap = new HashMap<>();
        for (ResearchSkillsCfg cfg : skillCfgList) {
            accumulateSectionDelta(deltaMap, cfg.getWinRate(), libType);
            accumulateSectionDelta(deltaMap, cfg.getSpecialModeProbUp(), libType);
        }
        return PropUtil.applyPropInfoDelta(propInfo, deltaMap);
    }

    /**
     * 合并配置中的区间权重修改
     *
     * @param deltaMap
     * @param cfgPropChangMap
     * @param libType
     */
    private void accumulateSectionDelta(Map<Integer, Integer> deltaMap, Map<Integer, Map<Integer, Integer>> cfgPropChangMap, int libType) {
        if (cfgPropChangMap == null || cfgPropChangMap.isEmpty()) {
            return;
        }
        Map<Integer, Integer> inner = cfgPropChangMap.get(libType);
        if (inner == null || inner.isEmpty()) {
            return;
        }
        inner.forEach((sectionIdx, delta) -> {
            if (sectionIdx == null || delta == null) {
                return;
            }
            deltaMap.merge(sectionIdx, delta, Integer::sum);
        });
    }

    /**
     * 使用技能
     *
     * @param simSkillsData
     * @param propInfo
     * @return
     */
    public PropInfo useLibTypeSkill(SimSkillsData simSkillsData, PropInfo propInfo) {
        if (simSkillsData == null) {
            return propInfo;
        }

        List<ResearchSkillsCfg> skillsCfgList = transSkills(simSkillsData);
        if (skillsCfgList.isEmpty()) {
            return propInfo;
        }

        PropInfo newPropInfo = applySpecialModeSkillBonus(propInfo, skillsCfgList);
        if (newPropInfo == null) {
            return propInfo;
        }
        return newPropInfo;
    }

    /**
     * 使用技能
     *
     * @param simSkillsData
     * @param propInfo
     * @return
     */
    public PropInfo useSectionSkill(SimSkillsData simSkillsData, PropInfo propInfo, int libType) {
        if (simSkillsData == null) {
            return propInfo;
        }

        List<ResearchSkillsCfg> skillsCfgList = transSkills(simSkillsData);
        if (skillsCfgList.isEmpty()) {
            return propInfo;
        }

        PropInfo newPropInfo = applySectionSkillBonus(propInfo, libType, skillsCfgList);
        if (newPropInfo == null) {
            return propInfo;
        }
        return newPropInfo;
    }

    /**
     * 将玩家身上的 propId->level 转化成 List<ResearchSkillsCfg>
     *
     * @param data
     * @return
     */
    public List<ResearchSkillsCfg> transSkills(SimSkillsData data) {
        Map<Integer, Map<Integer, ResearchSkillsCfg>> cfgMap = this.skillsCfgMap.get(data.getGameType());
        if (data.getSkillsMap() == null || data.getSkillsMap().isEmpty() || cfgMap == null || cfgMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<ResearchSkillsCfg> list = new ArrayList<>();
        for (Map.Entry<Integer, Integer> en : data.getSkillsMap().entrySet()) {
            Map<Integer, ResearchSkillsCfg> tmpMap = cfgMap.get(en.getKey());
            if (tmpMap != null && !tmpMap.isEmpty()) {
                ResearchSkillsCfg cfg = tmpMap.get(en.getValue());
                if (cfg != null) {
                    list.add(cfg);
                }
            }
        }
        return list;
    }
}
