package com.jjg.game.slots.game.captainjack.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;
import com.jjg.game.slots.game.captainjack.data.CaptainJackAddIconInfo;
import com.jjg.game.slots.game.captainjack.data.CaptainJackAwardLineInfo;
import com.jjg.game.slots.game.captainjack.data.CaptainJackResultLib;
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
public class CaptainJackGameGenerateManager extends AbstractSlotsGenerateManager<CaptainJackAwardLineInfo, CaptainJackResultLib> {
    public CaptainJackGameGenerateManager() {
        super(CaptainJackResultLib.class);
    }

    private int needTimes = 1;
    private int addTimes = 2;

    @Override
    public CaptainJackResultLib checkAward(int[] arr, CaptainJackResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查满线图案
        List<CaptainJackAwardLineInfo> fullLineInfoList = fullLine(lib);
        lib.addAllAwardLineInfo(fullLineInfoList);
        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        //存储消除后添加的图标
        List<CaptainJackAddIconInfo> addIconInfoList = new ArrayList<>();

        //拷贝数组
        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        //是否有消除
        repairIcons(newArr, lib.getAwardLineInfoList(), addIconInfoList);
        if (!addIconInfoList.isEmpty()) {
            lib.setAddIconInfos(addIconInfoList);
        }
        calTimes(lib);
        return lib;
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(CaptainJackResultLib lib) {
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }
        Set<Integer> libTypeSet = lib.getLibTypeSet();
        int[] arr = lib.getIconArr();
        //获取每个图标出现的次数
        Map<Integer, Set<Integer>> showCountMap = checkIconShowIndex(arr);
        log.debug("检查全局分散");
        //小游戏
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = new ArrayList<>();
        for (Map.Entry<Integer, BaseElementRewardCfg> en : normalRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();

            //检查出现的个数是否满足
            int elementsCount = 0;
            Set<Integer> indexSet = null;
            for (int iconId : cfg.getElementId()) {
                indexSet = showCountMap.get(iconId);
                if (indexSet != null) {
                    elementsCount += indexSet.size();
                }
            }
            if (indexSet == null || elementsCount != cfg.getRewardNum()) {
                continue;
            }
            if (lib.getJackpotId() > 0 && lib.getJackpotId() != cfg.getJackpotID()) {
                log.error("杰克船长出现奖池配置错误");
                throw new RuntimeException("杰克船长出现奖池配置错误");
            }
            //触发奖池
            if (cfg.getJackpotID() > 0) {
                lib.setJackpotId(cfg.getJackpotID());
            }

            //是否触发小游戏
            if (CollectionUtil.isEmpty(cfg.getFeatureTriggerId())) {
                continue;
            }
            CaptainJackAwardLineInfo captainJackAwardLineInfo = new CaptainJackAwardLineInfo();
            captainJackAwardLineInfo.setBaseTimes(0);
            captainJackAwardLineInfo.setSameIconSet(indexSet);
            captainJackAwardLineInfo.setSameIcon(cfg.getElementId().getFirst());
            lib.addAwardLineInfo(captainJackAwardLineInfo);
            Integer icon = cfg.getElementId().getFirst();
            cfg.getFeatureTriggerId().forEach(miniGameId -> {
                if (libTypeSet == null) {
                    targetSpecialAuxiliary(lib, miniGameId, icon, specialAuxiliaryInfoList);
                    return;
                }
                libTypeSet.forEach(libType -> {
                    targetSpecialAuxiliary(lib, miniGameId, icon, specialAuxiliaryInfoList);
                });

            });

        }
        return specialAuxiliaryInfoList;
    }

    private void targetSpecialAuxiliary(CaptainJackResultLib lib, Integer miniGameId, Integer icon, List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList) {
        if (icon == CaptainJackConstant.BaseElement.FREE_ICON) {
            SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerFree(lib, CaptainJackConstant.SpecialMode.FREE, miniGameId);
            if (specialAuxiliaryInfo != null) {
                if (specialAuxiliaryInfo.getFreeGames() != null && CollectionUtil.isEmpty(lib.getLibTypeSet())) {
                    if (lib.getSpecialAuxiliaryInfoList() == null) {
                        specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                        return;
                    }
                    //寻找第一个免费的specialAuxiliaryInfo
                    for (SpecialAuxiliaryInfo auxiliaryInfo : specialAuxiliaryInfoList) {
                        if (CollectionUtil.isNotEmpty(auxiliaryInfo.getFreeGames())) {
                            for (JSONObject freeGame : specialAuxiliaryInfo.getFreeGames()) {
                                specialAuxiliaryInfo.addFreeGame(freeGame);
                            }
                            break;
                        }
                    }
                    specialAuxiliaryInfo.setFreeGames(null);
                    lib.setAddFreeCount(specialAuxiliaryInfo.getFreeGames().size());
                    return;
                }
                specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
            }
        }
        if (icon == CaptainJackConstant.BaseElement.TREASURE_ICON) {
            triggerTreasureHuntMiniGame(lib, miniGameId);
        }
    }

    /**
     * 触发小游戏
     *
     * @param miniGameId
     * @return
     */
    public SpecialAuxiliaryInfo triggerFree(CaptainJackResultLib lib, int specialModeType, int miniGameId) {
        log.debug("触发小游戏 miniGameId = {}", miniGameId);
        //根据小游戏id去找相关配置
        SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
        if (specialAuxiliaryCfg == null) {
            log.warn("未找到该小游戏的配置 miniGameId = {}", miniGameId);
            return null;
        }

        SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(miniGameId);
        if (specialAuxiliaryPropConfig == null) {
            log.warn("未找到该小游戏小关的权重信息配置 miniGameId = {}", miniGameId);
            return null;
        }

        SpecialAuxiliaryInfo specialAuxiliaryInfo = new SpecialAuxiliaryInfo();
        specialAuxiliaryInfo.setCfgId(miniGameId);
        //检查免费旋转
        triggerFree(lib, specialModeType, specialAuxiliaryCfg, specialAuxiliaryPropConfig, specialAuxiliaryInfo);
        return specialAuxiliaryInfo;
    }

    /**
     * 触发探宝小游戏
     *
     * @param lib
     */
    private void triggerTreasureHuntMiniGame(CaptainJackResultLib lib, int specialAuxiliaryId) {
        SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(specialAuxiliaryId);
        if (specialAuxiliaryCfg == null) {
            log.error("杰克船长 未找到该小游戏的配置 miniGameId = {}", specialAuxiliaryId);
            return;
        }
        SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(specialAuxiliaryId);
        if (specialAuxiliaryPropConfig == null) {
            log.error("杰克船长 未找到该小游戏小关的权重信息配置 miniGameId = {}", specialAuxiliaryId);
            return;
        }
        if (lib.getDigTimes() > 0) {
            log.error("杰克船长 重复进行探宝游戏 miniGameId = {}", specialAuxiliaryId);
            throw new RuntimeException("重复进行探宝游戏");
        }
        //随机次数
        Integer digTimes = specialAuxiliaryPropConfig.getRandCountPropInfo().getRandKey();
        if (digTimes == null) {
            log.error("杰克船长 探宝游戏未找到随机次数 miniGameId = {}", specialAuxiliaryId);
            return;
        }
        lib.setDigTimes(digTimes);
        Map<Integer, Integer> map = new HashMap<>();
        while (lib.getDigTimesMultiplier() == null || lib.getDigTimesMultiplier().size() < digTimes) {
            //随机倍率
            Integer magnification = specialAuxiliaryPropConfig.getAwardTypeCPropInfo().getRandKey();
            if (magnification == null) {
                continue;
            }
            int maxShowLimit = specialAuxiliaryPropConfig.getAwardTypeCPropInfo().getMaxShowLimit(magnification);
            Integer merge = map.merge(magnification, 1, Integer::sum);
            if (merge > maxShowLimit) {
                continue;
            }
            lib.addDigTimesMultiplier(magnification);
        }

    }

    @Override
    protected CaptainJackAwardLineInfo addFullLineAwardInfo(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg) {
        CaptainJackAwardLineInfo info = new CaptainJackAwardLineInfo();
        info.setSameIconSet(sameIconIndexSet);
        info.setSameIcon(cfg.getElementId().getFirst());
        info.setBaseTimes(cfg.getBet());
        return info;
    }

    private void triggerFree(CaptainJackResultLib superiorLib, int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg,
                             SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }
        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }
        superiorLib.setAddFreeCount(freeCount);
        log.debug("增加免费游戏次数 addCount = {}", freeCount);
        for (int i = 0; i < freeCount; i++) {
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }
            CaptainJackResultLib lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = lib.getSpecialAuxiliaryInfoList();
            if (lib.getLibTypeSet() == null && CollectionUtil.isNotEmpty(specialAuxiliaryInfoList)) {
                lib.setSpecialAuxiliaryInfoList(null);
                specialAuxiliaryInfoList = List.copyOf(specialAuxiliaryInfoList);
                specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
                for (SpecialAuxiliaryInfo auxiliaryInfo : specialAuxiliaryInfoList) {
                    if (CollectionUtil.isNotEmpty(auxiliaryInfo.getFreeGames())) {
                        for (JSONObject freeGame : auxiliaryInfo.getFreeGames()) {
                            specialAuxiliaryInfo.addFreeGame(freeGame);
                        }
                    }
                }
            } else {
                specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
            }
        }

    }

    /**
     * 修补图标
     */
    public void repairIcons(int[] arr, List<CaptainJackAwardLineInfo> list, List<CaptainJackAddIconInfo> addIconInfoList) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        CaptainJackAddIconInfo addIconInfo = new CaptainJackAddIconInfo();

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);

        //将所有需要消除的图标进行汇总
        Map<Integer, Set<Integer>> allSameMap = new HashMap<>();
        for (CaptainJackAwardLineInfo info : list) {
            if (CollectionUtil.isEmpty(info.getSameIconSet())) {
                continue;
            }
            if (info.getSameIcon() == CaptainJackConstant.BaseElement.FREE_ICON || info.getSameIcon() == CaptainJackConstant.BaseElement.TREASURE_ICON) {
                continue;
            }
            for (Integer index : info.getSameIconSet()) {
                int columnId = index / baseInitCfg.getRows();
                if ((index % baseInitCfg.getRows()) != 0) {
                    columnId++;
                }
                allSameMap.computeIfAbsent(columnId, k -> new HashSet<>()).add(index);
            }
        }
        if (CollectionUtil.isEmpty(allSameMap)) {
            return;
        }
        //坐标对应添加的
        Map<Integer, Integer> addIconMap = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> en : allSameMap.entrySet()) {
            int colIndex = en.getKey();
            Set<Integer> set = en.getValue();
            //处理图标消除、下落和补充
            processIcons(colIndex, set, arr, addIconMap);
        }

        addIconInfo.setAddIconMap(addIconMap);
        //检查中奖
        List<CaptainJackAwardLineInfo> newAwardInfoList = fullLine(arr);

        addIconInfo.setAwardLineInfoList(newAwardInfoList);
        addIconInfoList.add(addIconInfo);
        repairIcons(arr, newAwardInfoList, addIconInfoList);
    }


    /**
     * 处理图标消除、下落和补充
     *
     * @param colIndex
     * @param removedIndexes 被消除的图标索引集合
     * @param arr
     * @return 新增的图标id
     */
    public void processIcons(int colIndex, Set<Integer> removedIndexes, int[] arr,
                             Map<Integer, Integer> addIconMap) {
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        int rows = baseInitCfg.getRows();

        int beginIndex = (colIndex - 1) * rows + 1;
        //这一列结束坐标
        int endIndex = beginIndex + rows - 1;
        //找到这一列，消除后应该剩余的图标
        List<Integer> validIndexes = new ArrayList<>(baseInitCfg.getRows() - removedIndexes.size());
        for (int i = beginIndex; i <= endIndex; i++) {
            int icon = arr[i];
            if (!removedIndexes.contains(i)) {
                validIndexes.add(icon);
            }
            arr[i] = -1;
        }
        validIndexes = validIndexes.reversed();
        //将剩余的图标重新填充回去
        int curIndex = endIndex;
        for (Integer validIndex : validIndexes) {
            arr[curIndex] = validIndex;
            curIndex--;
        }

        Map<Integer, BaseRollerCfg> rollerCfgMap = this.baseRollerCfgMap.values().iterator().next();
        BaseRollerCfg baseRollerCfg = rollerCfgMap.get(colIndex);
        int first = baseRollerCfg.getAxleCountScope().get(0) - 1;
        int last = baseRollerCfg.getAxleCountScope().get(1) - 1;
        int scopeIndex = RandomUtils.randomMinMax(first, last);

        // 从顶部开始补充新图标
        for (int i = 0; i < baseInitCfg.getRows(); i++) {
            if (scopeIndex > last) {
                scopeIndex = first;
            }
            int index = beginIndex + i;
            int oldIcon = arr[index];
            if (oldIcon > 0) {
                continue;
            }
            int elementId = baseRollerCfg.getElements().get(scopeIndex);
            arr[index] = elementId;
            addIconMap.put(index, elementId);
            log.debug("补充新图标 index = {}, icon = {}", index, elementId);
            scopeIndex++;
        }

    }


    @Override
    public void calTimes(CaptainJackResultLib lib) throws Exception {
        if (CollectionUtil.isEmpty(lib.getLibTypeSet())) {
            return;
        }
        if (CollectionUtil.isEmpty(lib.getSpecialAuxiliaryInfoList())) {
            //中奖线
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
            //消除后新增图标
            lib.addTimes(calAfterAddIcons(lib.getAddIconInfos()));
        } else {
            //计算免费游戏总倍数
            lib.addTimes(calFree(lib, Integer.MAX_VALUE));
        }
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<CaptainJackAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (CaptainJackAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    /**
     * 计算免费游戏的总倍数
     *
     * @param lib
     * @return
     */
    public long calFree(CaptainJackResultLib lib, int endIndex) {
        if (CollectionUtil.isEmpty(lib.getSpecialAuxiliaryInfoList())) {
            return 0;
        }
        long count = 0;
        long totalTimes = 0;
        for (SpecialAuxiliaryInfo info : lib.getSpecialAuxiliaryInfoList()) {
            if (CollectionUtil.isEmpty(info.getFreeGames())) {
                continue;
            }
            endIndex = Math.min(endIndex, info.getFreeGames().size());
            for (int i = 0; i < endIndex; i++) {
                int baseTimes = 1;
                JSONObject jsonObject = info.getFreeGames().get(i);
                CaptainJackResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), CaptainJackResultLib.class);
                if (CollectionUtil.isEmpty(tmpLib.getAddIconInfos())) {
                    continue;
                }
                //中奖线
                tmpLib.addTimes(calLineTimes(tmpLib.getAwardLineInfoList()));
                //消除后新增图标
                tmpLib.addTimes(calAfterAddIcons(tmpLib.getAddIconInfos()));
                if (tmpLib.getTimes() > 0) {
                    tmpLib.setTimes(tmpLib.getTimes() * (baseTimes + count / needTimes * addTimes));
                }
                jsonObject.put("times", tmpLib.getTimes());
                totalTimes += tmpLib.getTimes();
                count += tmpLib.getAddIconInfos().size();
            }
        }
        return totalTimes;
    }

    /**
     * 计算消除补齐后的中奖倍数
     *
     * @param addIconInfos
     * @return
     */
    public long calAfterAddIcons(List<CaptainJackAddIconInfo> addIconInfos) {
        if (CollectionUtil.isEmpty(addIconInfos)) {
            return 0;
        }
        long times = 0;
        for (CaptainJackAddIconInfo info : addIconInfos) {
            if (CollectionUtil.isEmpty(info.getAwardLineInfoList())) {
                continue;
            }
            for (CaptainJackAwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
                times += awardLineInfo.getBaseTimes();
            }
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(CaptainJackConstant.SpecialPlay.FREE_GAME_CONFIG_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] split = StringUtils.split(specialPlayCfg.getValue());
        if (split.length != 2) {
            return;
        }
        this.needTimes = Integer.parseInt(split[0]);
        this.addTimes = Integer.parseInt(split[1]);
    }

    public int getNeedTimes() {
        return needTimes;
    }

    public int getAddTimes() {
        return addTimes;
    }
}
