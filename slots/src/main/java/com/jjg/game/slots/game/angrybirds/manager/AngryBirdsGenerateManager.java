package com.jjg.game.slots.game.angrybirds.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.WeightRandom;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.angrybirds.constant.AngryBirdsConstant;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsAwardLineInfo;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsResultLib;
import com.jjg.game.slots.game.angrybirds.pb.bean.AngryBirdsReplaceInfo;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import jodd.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class AngryBirdsGenerateManager extends AbstractSlotsGenerateManager<AngryBirdsAwardLineInfo, AngryBirdsResultLib> {
    private final WeightRandom<Integer> freeMultiplier = new WeightRandom<>();

    public AngryBirdsGenerateManager() {
        super(AngryBirdsResultLib.class);
    }

    @Override
    public AngryBirdsResultLib checkAward(int[] arr, AngryBirdsResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);
        //检查连线
        List<AngryBirdsAwardLineInfo> awardLineInfoList = winLines(lib, freeModel);
        lib.setAwardLineInfoList(awardLineInfoList);
        //免费游戏计算随机乘倍值
        if (freeModel) {
            Integer next = freeMultiplier.next();
            if (next != null) {
                lib.setFreeMultiplier(next);
            }
        }
        calTimes(lib);
        return lib;
    }

    /**
     * 全局分散
     */
    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(AngryBirdsResultLib lib) {
        int[] arr = lib.getIconArr();
        Set<Integer> libTypeSet = lib.getLibTypeSet();
        List<SpecialGirdInfo> specialGirdInfoList = lib.getSpecialGirdInfoList();
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }
        //获取每个图标出现的次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(arr);

        log.debug("检查全局分散");

        //小游戏
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = new ArrayList<>();

        for (Map.Entry<Integer, BaseElementRewardCfg> en : normalRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();

            //检查出现的个数是否满足
            int elementsCount = 0;
            for (int iconId : cfg.getElementId()) {
                Integer count = showCountMap.get(iconId);
                if (count != null) {
                    elementsCount += count;
                }
            }
            if (elementsCount != cfg.getRewardNum()) {
                continue;
            }
            if (cfg.getJackpotID() > 0) {
                lib.addJackpotId(cfg.getJackpotID());
            }
            //是否触发小游戏
            if (CollectionUtil.isEmpty(cfg.getFeatureTriggerId())) {
                continue;
            }
            Set<Integer> triggerFreeSet;
            if (CollectionUtil.isEmpty(libTypeSet)) {
                triggerFreeSet = SlotsConst.specialModeTriggerFreeModeIds.get(this.gameType);
            } else {
                triggerFreeSet = libTypeSet;
            }
            if (triggerFreeSet != null && !triggerFreeSet.isEmpty()) {
                cfg.getFeatureTriggerId().forEach(miniGameId -> {
                    triggerFreeSet.forEach(libType -> {
                        if (miniGameId == AngryBirdsConstant.Common.SPECIAL_AUXILIARY_ID) {
                            triggerAuxiliaryExtra(lib, miniGameId);
                            return;
                        }
                        SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(libType, arr, miniGameId, specialGirdInfoList);
                        if (specialAuxiliaryInfo != null) {
                            specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                        }
                    });

                });
            }
        }
        return specialAuxiliaryInfoList;
    }

    protected void triggerAuxiliaryExtra(AngryBirdsResultLib lib, int miniGameId) {
        //根据小游戏id去找相关配置
        SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
        if (specialAuxiliaryCfg == null) {
            log.warn("未找到该小游戏的配置 miniGameId = {}", miniGameId);
            return;
        }
        SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(miniGameId);
        if (specialAuxiliaryPropConfig == null) {
            log.warn("未找到该小游戏小关的权重信息配置 miniGameId = {}", miniGameId);
            return;
        }
        if (specialAuxiliaryPropConfig.getRandCountPropInfo() == null) {
            return;
        }

        List<Integer> specialGirdIdList = specialAuxiliaryCfg.getSpecialGirdID();

        if (CollectionUtil.isEmpty(specialGirdIdList)) {
            return;
        }
        for (int specialGirdCfgId : specialAuxiliaryCfg.getSpecialGirdID()) {
            int[] old = Arrays.copyOfRange(lib.getIconArr(), 1, lib.getIconArr().length);
            SpecialGirdInfo specialGirdInfo = gridUpdate(specialGirdCfgId, lib.getIconArr());
            for (Map.Entry<Integer, Integer> entry : specialGirdInfo.getValueMap().entrySet()) {
                AngryBirdsReplaceInfo replaceInfo = new AngryBirdsReplaceInfo();
                replaceInfo.index = entry.getKey();
                replaceInfo.newIcon = entry.getValue();
                replaceInfo.oldIcon = old[replaceInfo.index];
                lib.addAngryBirdsReplaceInfo(replaceInfo);
            }
        }
    }

    @Override
    protected AngryBirdsAwardLineInfo getAwardLineInfo() {
        return new AngryBirdsAwardLineInfo();
    }

    @Override
    public void calTimes(AngryBirdsResultLib lib) throws Exception {
        if (lib.getFreeMultiplier() > 0 && CollectionUtil.isNotEmpty(lib.getAwardLineInfoList())) {
            for (AngryBirdsAwardLineInfo lineInfo : lib.getAwardLineInfoList()) {
                lineInfo.setBaseTimes(lineInfo.getBaseTimes() * lib.getFreeMultiplier());
            }
        }
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<AngryBirdsAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int times = 0;
        for (AngryBirdsAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }


    @Override
    protected void specialPlayConfig() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(AngryBirdsConstant.SpecialPlay.FREE_GAME_CONFIG_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] config = StringUtils.split(specialPlayCfg.getValue(), "|");
        if (config.length == 0) {
            return;
        }
        freeMultiplier.clear();
        for (String freeCfg : config) {
            String[] split = StringUtils.split(freeCfg, "_");
            if (split.length != 2) {
                continue;
            }
            freeMultiplier.add(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        }
    }

}
