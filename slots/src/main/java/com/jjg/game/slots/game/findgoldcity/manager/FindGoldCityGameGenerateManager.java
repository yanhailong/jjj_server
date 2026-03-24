package com.jjg.game.slots.game.findgoldcity.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.WeightRandom;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.data.PropInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.findgoldcity.constant.FindGoldCityConstant;
import com.jjg.game.slots.game.findgoldcity.data.FindGoldCityAddIconInfo;
import com.jjg.game.slots.game.findgoldcity.data.FindGoldCityAwardLineInfo;
import com.jjg.game.slots.game.findgoldcity.data.FindGoldCityResultLib;
import com.jjg.game.slots.manager.MultiGridSlotsGenerateManager;
import jodd.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class FindGoldCityGameGenerateManager extends MultiGridSlotsGenerateManager<FindGoldCityAwardLineInfo, FindGoldCityResultLib> {
    //模式->增加倍数->最大倍数
    private Map<Integer, Pair<Integer, Integer>> bonusAccumulationMap = Map.of();
    //元素id->百搭符号权重_黏性百搭符号权重
    private Map<Integer, WeightRandom<Integer>> goldSymbolConversion = Map.of();
    public FindGoldCityGameGenerateManager() {
        super(FindGoldCityResultLib.class);
    }

    @Override
    public FindGoldCityResultLib checkAward(int[] arr, FindGoldCityResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查满线图案
        List<FindGoldCityAwardLineInfo> fullLineInfoList = fullLine(lib);
        lib.addAllAwardLineInfo(fullLineInfoList);
        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        //存储消除后添加的图标
        List<FindGoldCityAddIconInfo> addIconInfoList = new ArrayList<>();

        //拷贝数组
        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        //是否有消除
        repairIcons(newArr, lib.getAwardLineInfoList(), addIconInfoList, new HashMap<>());
        if (!addIconInfoList.isEmpty()) {
            lib.setAddIconInfos(addIconInfoList);
        }
        //根据模式重新计算倍数
        if (!freeModel) {
            List<JSONObject> freeObject = new ArrayList<>();
            mergeFreeResults(lib, freeObject, true);
            for (Integer libType : lib.getLibTypeSet()) {
                //免费的是累计的
                Pair<Integer, Integer> pair = bonusAccumulationMap.get(libType);
                if (pair == null) {
                    continue;
                }
                int base = pair.getFirst();
                if (libType == FindGoldCityConstant.SpecialMode.FREE) {
                    for (JSONObject jsonObject : freeObject) {
                        FindGoldCityResultLib freeLib = jsonObject.toJavaObject(FindGoldCityResultLib.class);
                        jsonObject.put("currentMultiple", base);
                        base = calculateAwardLine(freeLib.getAwardLineInfoList(), base, pair);
                        jsonObject.put("awardLineInfoList", freeLib.getAwardLineInfoList());
                        //掉落中奖
                        base = calculateEliminationFactor(freeLib, base, pair);
                        jsonObject.put("addIconInfos", freeLib.getAddIconInfos());
                        freeLib.setTimes(0);
                        calTimes(freeLib);
                        jsonObject.put("times", freeLib.getTimes());
                    }
                } else {
                    //计算初次
                    base = calculateAwardLine(lib.getAwardLineInfoList(), base, pair);
                    calculateEliminationFactor(lib, base, pair);
                }
            }
        }
        calTimes(lib);
        return lib;
    }

    @Override
    public boolean canDoReplace(BaseInitCfg baseInitCfg, SpecialGirdCfg specialGirdCfg, int[] arrIcon, int needRow, int girdId) {
        //计算格子所在行信息,判断上面的元素是否能替换，以及是否有足够多的元素支持替换
        int rows = baseInitCfg.getRows() - 1;
        int currentRow = (girdId - 1) % rows + 1;
        if (currentRow < needRow) {
            return false;
        }
        for (int i = 1; i < needRow; i++) {
            int tempIcon = arrIcon[girdId - i];
            if (specialGirdCfg.getNotReplaceEle() != null && specialGirdCfg.getNotReplaceEle().contains(tempIcon)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算中奖线的倍数
     *
     * @param lib  结果库
     * @param base 基础倍数
     * @param pair 倍数基数
     * @return 基础倍数
     */
    private int calculateAwardLine(List<FindGoldCityAwardLineInfo> lib, int base, Pair<Integer, Integer> pair) {
        if (CollectionUtil.isNotEmpty(lib)) {
            for (FindGoldCityAwardLineInfo cityAwardLineInfo : lib) {
                cityAwardLineInfo.setBaseTimes(cityAwardLineInfo.getBaseTimes() * base);
                base = Math.min(base + base, pair.getSecond());
            }
        }
        return base;
    }

    /**
     * 计算消除后的倍数
     *
     * @param lib  结果库
     * @param base 基础倍数
     * @param pair 倍数基数
     * @return 基础倍数
     */
    private int calculateEliminationFactor(FindGoldCityResultLib lib, int base, Pair<Integer, Integer> pair) {
        if (CollectionUtil.isNotEmpty(lib.getAddIconInfos())) {
            for (FindGoldCityAddIconInfo addIconInfo : lib.getAddIconInfos()) {
                base = calculateAwardLine(addIconInfo.getAwardLineInfoList(), base, pair);
            }
        }
        return base;
    }


    @Override
    public void onMergeFreeResults(FindGoldCityResultLib lib, int addCount) {
        lib.setAddFreeCount(addCount);
    }

    /**
     * 修补图标
     */
    public void repairIcons(int[] arr, List<FindGoldCityAwardLineInfo> list, List<FindGoldCityAddIconInfo> addIconInfoList, Map<Integer, Integer> remainTimes) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        FindGoldCityAddIconInfo addIconInfo = new FindGoldCityAddIconInfo();

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        Map<Integer, Integer> addIconMap = new HashMap<>();
        Set<Integer> processedRemainIndexes = new HashSet<>();

        //将所有需要消除的图标进行汇总
        Map<Integer, Set<Integer>> allSameMap = new HashMap<>();
        for (FindGoldCityAwardLineInfo info : list) {
            if (CollectionUtil.isEmpty(info.getSameIconSet())) {
                continue;
            }
            for (Integer index : info.getSameIconSet()) {
                if (remainTimes.containsKey(index)) {
                    // 黏性 wild 在同一轮 cascade 里命中多条线时，只扣一次持续次数
                    if (!processedRemainIndexes.add(index)) {
                        continue;
                    }
                    int remainTime = remainTimes.get(index);
                    if (remainTime > 0) {
                        remainTimes.put(index, remainTime - 1);
                        continue;
                    }
                }
                //取具体的元素id
                int elementId = arr[index];
                //进行图标变化
                PropInfo propInfo = baseElementPostChangeMap.get(elementId);
                if (propInfo != null) {
                    Integer newIcon = propInfo.getRandKey();
                    WeightRandom<Integer> random = goldSymbolConversion.get(newIcon);
                    if (random != null) {
                        Integer next = random.next();
                        remainTimes.put(index, next);
                    }
                    // 转换出的 wild/黏性 wild 需要立即写回当前盘面，参与后续连消判奖
                    arr[index] = newIcon;
                    addIconMap.put(index, newIcon);
                    continue;
                }
                int columnId = index / baseInitCfg.getRows();
                if ((index % baseInitCfg.getRows()) != 0) {
                    columnId++;
                }
                //多格的掉落直接全部添加
                BaseElementCfg baseElementCfg = getBaseElementCfgMap().get(elementId);
                int baseLoop = 1;
                if (baseElementCfg != null && baseElementCfg.getSpace() > 1) {
                    baseLoop = baseElementCfg.getSpace();
                }
                for (int i = 0; i < baseLoop; i++) {
                    allSameMap.computeIfAbsent(columnId, k -> new HashSet<>()).add(index - i);
                }
            }
        }
        if (CollectionUtil.isEmpty(allSameMap) && addIconMap.isEmpty()) {
            return;
        }
        //坐标对应添加的
        for (Map.Entry<Integer, Set<Integer>> en : allSameMap.entrySet()) {
            int colIndex = en.getKey();
            Set<Integer> set = en.getValue();
            //处理图标消除、下落和补充
            processIcons(colIndex, set, arr, addIconMap, remainTimes);
        }

        addIconInfo.setAddIconMap(addIconMap);
        //检查中奖
        List<FindGoldCityAwardLineInfo> newAwardInfoList = fullLine(arr);
        //减次数
        if (CollectionUtil.isNotEmpty(remainTimes)) {
            for (FindGoldCityAwardLineInfo cityAwardLineInfo : newAwardInfoList) {
                for (Integer icon : cityAwardLineInfo.getSameIconSet()) {
                    if (remainTimes.containsKey(icon)) {
                        cityAwardLineInfo.setElementRemainTimes(Map.copyOf(remainTimes));
                        break;
                    }
                }
            }
        }
        addIconInfo.setAwardLineInfoList(newAwardInfoList);
        addIconInfoList.add(addIconInfo);

        repairIcons(arr, newAwardInfoList, addIconInfoList, remainTimes);
    }

    /**
     * 处理图标消除、下落和补充
     *
     * @param colIndex       行索引
     * @param removedIndexes 被消除的图标索引集合
     * @param arr            图标数组
     */
    public void processIcons(int colIndex, Set<Integer> removedIndexes, int[] arr,
                             Map<Integer, Integer> addIconMap, Map<Integer, Integer> remainTimes) {
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        int rows = baseInitCfg.getRows();

        int beginIndex = (colIndex - 1) * rows + 1;
        //这一列结束坐标
        int endIndex = beginIndex + rows - 1;
        //找到这一列，消除后应该剩余的图标
        List<Integer> validIndexes = new ArrayList<>(baseInitCfg.getRows() - removedIndexes.size());
        List<Integer> validRemainTimes = new ArrayList<>(baseInitCfg.getRows() - removedIndexes.size());
        for (int i = beginIndex; i <= endIndex; i++) {
            int icon = arr[i];
            Integer remainTime = remainTimes.remove(i);
            if (!removedIndexes.contains(i)) {
                validIndexes.add(icon);
                validRemainTimes.add(remainTime);
            }
            arr[i] = -1;
        }
        validIndexes = validIndexes.reversed();
        validRemainTimes = validRemainTimes.reversed();
        //将剩余的图标重新填充回去
        int curIndex = endIndex;
        for (int i = 0; i < validIndexes.size(); i++) {
            Integer validIndex = validIndexes.get(i);
            arr[curIndex] = validIndex;
            Integer remainTime = validRemainTimes.get(i);
            if (remainTime != null) {
                remainTimes.put(curIndex, remainTime);
            }
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
            if (oldIcon >= 0) {
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
    protected FindGoldCityAwardLineInfo getAwardLineInfo() {
        return new FindGoldCityAwardLineInfo();
    }


    @Override
    public void calTimes(FindGoldCityResultLib lib) throws Exception {
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        //消除图标
        lib.addTimes(calAfterAddIcons(lib.getAddIconInfos()));
    }

    /**
     * 计算消除补齐后的中奖倍数
     *
     */
    public long calAfterAddIcons(List<FindGoldCityAddIconInfo> addIconInfos) {
        if (CollectionUtil.isEmpty(addIconInfos)) {
            return 0;
        }
        long times = 0;
        for (FindGoldCityAddIconInfo info : addIconInfos) {
            if (CollectionUtil.isEmpty(info.getAwardLineInfoList())) {
                continue;
            }
            for (FindGoldCityAwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
                times += awardLineInfo.getBaseTimes();
            }
        }
        return times;
    }

    /**
     * 计算中奖线的倍数
     *
     */
    public int calLineTimes(List<FindGoldCityAwardLineInfo> list) {
        if (CollectionUtil.isEmpty(list)) {
            return 0;
        }
        int times = 0;
        for (FindGoldCityAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        loadBonusAccumulation();

        loadGoldSymbolConversion();

    }

    private void loadBonusAccumulation() {
        //1,1_10;2,2_20
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(FindGoldCityConstant.Common.BONUS_ACCUMULATION_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] modeArr = specialPlayCfg.getValue().split(";");
        if (modeArr.length != 2) {
            return;
        }
        Map<Integer, Pair<Integer, Integer>> tempMap = new HashMap<>(2);
        for (String cfg : modeArr) {
            String[] cfg1 = cfg.split(",");
            if (cfg1.length != 2) {
                continue;
            }
            String[] cfg2 = cfg1[1].split("_");
            tempMap.put(Integer.parseInt(cfg1[0]), Pair.newPair(Integer.parseInt(cfg2[0]), Integer.parseInt(cfg2[1])));
        }
        bonusAccumulationMap = tempMap;
    }


    private void loadGoldSymbolConversion() {
        //29_9_1|41_9_1|53_9_1
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(FindGoldCityConstant.Common.GOLD_SYMBOL_CONVERSION_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] split = StringUtils.split(specialPlayCfg.getValue(), "|");
        if (split.length == 0) {
            return;
        }
        Map<Integer, WeightRandom<Integer>> temp = new HashMap<>(split.length);
        for (String cfg : split) {
            String[] split1 = StringUtils.split(cfg, "_");
            if (split1.length != 3) {
                continue;
            }
            WeightRandom<Integer> random = new WeightRandom<>();
            random.add(1, Integer.parseInt(split1[1]));
            random.add(3, Integer.parseInt(split1[2]));
            temp.put(Integer.parseInt(split1[0]), random);
        }
        goldSymbolConversion = temp;
    }

}
