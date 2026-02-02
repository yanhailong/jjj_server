package com.jjg.game.slots.game.elephantgod.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.captainjack.data.CaptainJackAwardLineInfo;
import com.jjg.game.slots.game.captainjack.data.CaptainJackResultLib;
import com.jjg.game.slots.game.elephantgod.ElephantGodConstant;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodAwardLineInfo;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ElephantGodGenerateManager extends AbstractSlotsGenerateManager<ElephantGodAwardLineInfo, ElephantGodResultLib> {
    private int basicMultiplier = 2;
    private int addMultiplier = 2;
    private int needWildCount = 3;
    private int maxMultiplier = 20;

    public ElephantGodGenerateManager() {
        super(ElephantGodResultLib.class);
    }

    @Override
    public ElephantGodResultLib checkAward(int[] arr, ElephantGodResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查满线图案
        List<ElephantGodAwardLineInfo> fullLineInfoList = fullLine(lib);
        lib.addAllAwardLineInfo(fullLineInfoList);
        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);
        calTimes(lib);
        return lib;
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<ElephantGodAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int times = 0;
        for (ElephantGodAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    @Override
    public void calTimes(ElephantGodResultLib lib) throws Exception {
        //修正免费模式中奖线信息
        if (CollectionUtil.isNotEmpty(lib.getSpecialAuxiliaryInfoList())) {
            for (SpecialAuxiliaryInfo auxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
                if (CollectionUtil.isNotEmpty(auxiliaryInfo.getFreeGames())) {
                    int basicMultiplier = this.basicMultiplier;
                    int wildCount = 0;
                    List<JSONObject> freeGames = auxiliaryInfo.getFreeGames();
                    for (int i = 0; i < freeGames.size(); i++) {
                        JSONObject freeGame = freeGames.get(i);
                        ElephantGodResultLib freeResultLib = freeGame.toJavaObject(ElephantGodResultLib.class);
                        //检查wild数量
                        wildCount += checkWildCount(freeResultLib.getIconArr());
                        if (wildCount >= needWildCount) {
                            basicMultiplier += addMultiplier;
                            wildCount = 0;
                        }
                        if (CollectionUtil.isNotEmpty(freeResultLib.getAwardLineInfoList())) {
                            for (ElephantGodAwardLineInfo awardLineInfo : freeResultLib.getAwardLineInfoList()) {
                                awardLineInfo.setBaseTimes(awardLineInfo.getBaseTimes() * basicMultiplier);
                            }
                        }
                        freeResultLib.setBasicMultiplier(basicMultiplier);
                        freeResultLib.setWildCount(wildCount);
                        auxiliaryInfo.getFreeGames().set(i, (JSONObject) JSON.toJSON(freeResultLib));
                    }

                }
            }
        }
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
    }

    private int checkWildCount(int[] arr) {
        int wildCount = 0;
        for (int j : arr) {
            if (j == ElephantGodConstant.BaseElement.ID_WILD) {
                wildCount++;
            }
        }
        return wildCount;
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(ElephantGodResultLib lib) {
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }
        Set<Integer> libTypeSet = lib.getLibTypeSet();
        int[] arr = lib.getIconArr();
        List<SpecialGirdInfo> specialGirdInfoList = lib.getSpecialGirdInfoList();
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
            if (lib.getJackpotIds() != null && !lib.getJackpotIds().isEmpty() && !lib.getJackpotIds().contains(cfg.getJackpotID())) {
                log.error("象财神出现奖池配置错误");
                throw new RuntimeException("象财神出现奖池配置错误");
            }
            //触发奖池
            if (cfg.getJackpotID() > 0) {
                lib.addJackpotId(cfg.getJackpotID());
            }

            //是否触发小游戏
            if (CollectionUtil.isEmpty(cfg.getFeatureTriggerId())) {
                continue;
            }

            if (libTypeSet == null || libTypeSet.isEmpty()) {
                Set<Integer> triggerFreeSet = SlotsConst.specialModeTriggerFreeModeIds.get(this.gameType);
                if (triggerFreeSet != null && !triggerFreeSet.isEmpty()) {
                    cfg.getFeatureTriggerId().forEach(miniGameId -> {
                        triggerFreeSet.forEach(libType -> {
                            SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerFreeGame(lib, libType, miniGameId, specialGirdInfoList);
                            if (specialAuxiliaryInfo != null) {
                                specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                            }
                        });

                    });
                }
            } else {
                cfg.getFeatureTriggerId().forEach(miniGameId -> {
                    libTypeSet.forEach(libType -> {
                        SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerFreeGame(lib, libType, miniGameId, specialGirdInfoList);
                        if (specialAuxiliaryInfo != null) {
                            specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                        }
                    });

                });
            }
        }
        return specialAuxiliaryInfoList;
    }

    public SpecialAuxiliaryInfo triggerFreeGame(ElephantGodResultLib lib, int specialModeType, int miniGameId, List<SpecialGirdInfo> specialGirdInfoList) {
        log.debug("触发小游戏 miniGameId = {}", miniGameId);
        //根据小游戏id去找相关配置
        SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
        if (specialAuxiliaryCfg == null) {
            log.warn("未找到该小游戏的配置 miniGameId = {}", miniGameId);
            return null;
        }

        SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(miniGameId);
        if (specialAuxiliaryPropConfig == null || specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            log.warn("未找到该小游戏小关的权重信息配置 miniGameId = {}", miniGameId);
            return null;
        }
        SpecialAuxiliaryInfo specialAuxiliaryInfo = new SpecialAuxiliaryInfo();
        specialAuxiliaryInfo.setCfgId(miniGameId);

        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return null;
        }
        lib.setAddFreeCount(freeCount);
        for (int i = 0; i < freeCount; i++) {
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }

            ElephantGodResultLib resultLib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            if (CollectionUtil.isNotEmpty(resultLib.getSpecialAuxiliaryInfoList())) {
                specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(resultLib));
                for (SpecialAuxiliaryInfo auxiliaryInfo : resultLib.getSpecialAuxiliaryInfoList()) {
                    if (CollectionUtil.isNotEmpty(auxiliaryInfo.getFreeGames())) {
                        for (JSONObject freeGame : auxiliaryInfo.getFreeGames()) {
                            specialAuxiliaryInfo.addFreeGame(freeGame);
                        }
                    }
                }
            }
        }
        return specialAuxiliaryInfo;
    }
}