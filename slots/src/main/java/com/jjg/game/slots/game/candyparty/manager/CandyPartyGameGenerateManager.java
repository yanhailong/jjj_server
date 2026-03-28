package com.jjg.game.slots.game.candyparty.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRollerCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialModeCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.game.candyparty.constant.CandyPartyConstant;
import com.jjg.game.slots.game.candyparty.data.CandyPartyAddIconInfo;
import com.jjg.game.slots.game.candyparty.data.CandyPartyAwardLineInfo;
import com.jjg.game.slots.game.candyparty.data.CandyPartyResultLib;
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
public class CandyPartyGameGenerateManager extends AbstractSlotsGenerateManager<CandyPartyAwardLineInfo, CandyPartyResultLib> {
    //modelId->层数
    private final Map<Integer, Integer> modelLayer;
    //过关条件 modelId->需要元素Id->需要数量
    private Map<Integer, Pair<Integer, Integer>> passingCriteriaMap = Map.of();
    public CandyPartyGameGenerateManager() {
        super(CandyPartyResultLib.class);
        modelLayer = new HashMap<>();
        CandyPartyConstant.SpecialMode.FREE_MAP.forEach((k, v) -> modelLayer.put(v, k));
        CandyPartyConstant.SpecialMode.NORMAL_MAP.forEach((k, v) -> modelLayer.put(v, k));
        CandyPartyConstant.SpecialMode.JACKPOT_MAP.forEach((k, v) -> modelLayer.put(v, k));
    }

    @Override
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        int specialAuxiliaryCfgId = CandyPartyConstant.SpecialMode.AUXILIARY_MAP.getOrDefault(specialModeType, 0);
        if (specialAuxiliaryCfg.getId() != specialAuxiliaryCfgId) {
            return;
        }
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }
        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        for (int i = 0; i < freeCount; i++) {
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }

            CandyPartyResultLib t = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            if (t == null) {
                log.error("糖果派对生成免费结果库失败,specialModeType:{} ", specialModeType);
                continue;
            }
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(t));
        }
    }

    @Override
    protected void specialPlayConfig() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(CandyPartyConstant.SpecialPlay.PASSING_CRITERIA_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            passingCriteriaMap = Map.of();
            return;
        }
        String[] split = StringUtils.split(specialPlayCfg.getValue(), "|");
        if (split.length != 3) {
            passingCriteriaMap = Map.of();
            return;
        }
        Map<Integer, Pair<Integer, Integer>> tempMap = new HashMap<>();
        for (String detailCfg : split) {
            String[] cfg = StringUtils.split(detailCfg, "_");
            if (cfg.length != 3) {
                continue;
            }
            tempMap.put(Integer.parseInt(cfg[0]), Pair.newPair(Integer.parseInt(cfg[1]), Integer.parseInt(cfg[2])));
        }
        passingCriteriaMap = tempMap;
    }

    public Map<Integer, Pair<Integer, Integer>> getPassingCriteriaMap() {
        return passingCriteriaMap == null ? Map.of() : passingCriteriaMap;
    }

    @Override
    public CandyPartyResultLib checkAward(int[] arr, CandyPartyResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        Integer libType = lib.getLibTypeSet().iterator().next();
        SpecialModeCfg specialModeCfg = specialModeCfgMap.get(libType);
        int rows = specialModeCfg.getRows();
        int cols = specialModeCfg.getCols();

        List<CandyPartyAwardLineInfo> candyPartyAwardLineInfos = checkAssignPatternAward(arr, rows, cols);
        lib.setAwardLineInfoList(candyPartyAwardLineInfos);

        List<SpecialAuxiliaryInfo> specialAuxiliaryInfos = overallDisperse(lib);
        lib.setSpecialAuxiliaryInfoList(specialAuxiliaryInfos);
        //存储消除后添加的图标
        List<CandyPartyAddIconInfo> addIconInfoList = new ArrayList<>();
        //拷贝数组
        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        //是否有消除
        List<CandyPartyAwardLineInfo> awardLineInfoList = lib.getAwardLineInfoList();
        repairIcons(cols, rows, newArr, awardLineInfoList, addIconInfoList);
        if (!addIconInfoList.isEmpty()) {
            lib.setAddIconInfos(addIconInfoList);
        }
        //遍历中奖图标
        if (CollectionUtil.isNotEmpty(awardLineInfoList)) {
            int addCount = 0;
            int layer = modelLayer.getOrDefault(libType, 0);
            Pair<Integer, Integer> pair = passingCriteriaMap.get(layer);
            if (pair != null) {
                for (CandyPartyAwardLineInfo info : awardLineInfoList) {
                    if (info.getSameIcon() == pair.getFirst()) {
                        addCount++;
                    }
                }
                if (CollectionUtil.isNotEmpty(lib.getAddIconInfos())) {
                    for (CandyPartyAddIconInfo addIconInfo : lib.getAddIconInfos()) {
                        if (CollectionUtil.isEmpty(addIconInfo.getAwardLineInfoList())) {
                            continue;
                        }
                        for (CandyPartyAwardLineInfo info : addIconInfo.getAwardLineInfoList()) {
                            if (info.getSameIcon() == pair.getFirst()) {
                                addCount++;
                            }
                        }
                    }
                }
            }
            lib.setElementCollectionNum(addCount);
        }
        if (CollectionUtil.isNotEmpty(lib.getSpecialAuxiliaryInfoList())) {
            int multiply = 0;
            for (SpecialAuxiliaryInfo auxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
                if (CollectionUtil.isEmpty(auxiliaryInfo.getFreeGames())) {
                    continue;
                }
                multiply = RandomUtil.randomInt(5) + 1;
                //重新计算免费模式的倍率
                for (JSONObject freeGame : auxiliaryInfo.getFreeGames()) {
                    CandyPartyResultLib resultLib = freeGame.toJavaObject(CandyPartyResultLib.class);
                    if (resultLib.getTimes() == 0) {
                        continue;
                    }
                    long newTimes = calAfterAddIcons(resultLib.getAddIconInfos(), multiply);
                    newTimes += calLineTimes(resultLib.getAwardLineInfoList(), multiply);
                    freeGame.put("addIconInfos", resultLib.getAddIconInfos());
                    freeGame.put("awardLineInfoList", resultLib.getAwardLineInfoList());
                    freeGame.put("times", newTimes);
                }
            }
            lib.setFreeGameMultiple(multiply);
        }
        calTimes(lib);
        return lib;
    }

    @Override
    public boolean autoSetFreeModelLibType() {
        return true;
    }

    @Override
    protected CandyPartyAwardLineInfo getAwardLineInfo() {
        return new CandyPartyAwardLineInfo();
    }

    /**
     * 修补图标
     */
    public void repairIcons(int cols, int rows, int[] arr, List<CandyPartyAwardLineInfo> list, List<CandyPartyAddIconInfo> addIconInfoList) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        CandyPartyAddIconInfo addIconInfo = new CandyPartyAddIconInfo();
        //将所有需要消除的图标进行汇总
        Map<Integer, Set<Integer>> allSameMap = new HashMap<>();
        for (CandyPartyAwardLineInfo info : list) {
            if (CollectionUtil.isEmpty(info.getSameIconSet())) {
                continue;
            }
            for (Integer index : info.getSameIconSet()) {
                int columnId = index / rows;
                if ((index % rows) != 0) {
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
            processIcons(rows, colIndex, set, arr, addIconMap);
        }

        addIconInfo.setAddIconMap(addIconMap);
        //检查中奖
        List<CandyPartyAwardLineInfo> newAwardInfoList = new ArrayList<>();

        List<CandyPartyAwardLineInfo> candyPartyAwardLineInfos = checkAssignPatternAward(arr, rows, cols);
        if (CollectionUtil.isNotEmpty(candyPartyAwardLineInfos)) {
            newAwardInfoList.addAll(candyPartyAwardLineInfos);
        }
        addIconInfo.setAwardLineInfoList(newAwardInfoList);
        addIconInfoList.add(addIconInfo);
        repairIcons(cols, rows, arr, newAwardInfoList, addIconInfoList);
    }


    /**
     * 处理图标消除、下落和补充
     *
     * @param colIndex
     * @param removedIndexes 被消除的图标索引集合
     * @param arr
     * @return 新增的图标id
     */
    public void processIcons(int rows, int colIndex, Set<Integer> removedIndexes, int[] arr,
                             Map<Integer, Integer> addIconMap) {
        int beginIndex = (colIndex - 1) * rows + 1;
        //这一列结束坐标
        int endIndex = beginIndex + rows - 1;
        //找到这一列，消除后应该剩余的图标
        List<Integer> validIndexes = new ArrayList<>(rows - removedIndexes.size());
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
        for (int i = 0; i < rows; i++) {
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
    public void calTimes(CandyPartyResultLib lib) throws Exception {
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList(), 1));
        //消除后新增图标
        lib.addTimes(calAfterAddIcons(lib.getAddIconInfos(), 1));
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<CandyPartyAwardLineInfo> list, int multiple) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int times = 0;
        for (CandyPartyAwardLineInfo awardLineInfo : list) {
            awardLineInfo.setBaseTimes(awardLineInfo.getBaseTimes() * multiple);
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }


    /**
     * 计算消除补齐后的中奖倍数
     *
     * @param addIconInfos
     * @return
     */
    public long calAfterAddIcons(List<CandyPartyAddIconInfo> addIconInfos, int multiple) {
        if (CollectionUtil.isEmpty(addIconInfos)) {
            return 0;
        }
        long times = 0;
        for (CandyPartyAddIconInfo info : addIconInfos) {
            if (CollectionUtil.isEmpty(info.getAwardLineInfoList())) {
                continue;
            }
            for (CandyPartyAwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
                awardLineInfo.setBaseTimes(awardLineInfo.getBaseTimes() * multiple);
                times += awardLineInfo.getBaseTimes();
            }
        }
        return times;
    }

}
