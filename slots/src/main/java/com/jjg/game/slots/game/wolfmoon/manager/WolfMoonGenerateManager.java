package com.jjg.game.slots.game.wolfmoon.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRollerCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonAddIconInfo;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonAwardLineInfo;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import jodd.util.StringUtil;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@Component
public class WolfMoonGenerateManager extends AbstractSlotsGenerateManager<WolfMoonAwardLineInfo, WolfMoonResultLib> {

    public WolfMoonGenerateManager() {
        super(WolfMoonResultLib.class);
    }
    @Override
    public void changeSampleCallbackCollector() {
        log.warn("狼月 无法重载配置表");
    }

    //固定堆叠百搭符号 不消除
    private final Set<Integer> freeImmutableElements = Set.of(WolfMoonConstant.BaseElement.WILD);
    //基础倍数/乘倍数->最大倍数
    private Pair<Integer, Integer> freeAddCfg;
    //图标icon 增加次数
    private Pair<Integer, Integer> freeIconAddCfg;

    @Override
    public WolfMoonResultLib checkAward(int[] arr, WolfMoonResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);

        //检查满线图案_x连
        List<WolfMoonAwardLineInfo> fullLineInfoList = fullLine(lib);
        lib.addAllAwardLineInfo(fullLineInfoList);

        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        //存储消除后添加的图标
        List<WolfMoonAddIconInfo> addIconInfoList = new ArrayList<>();
        //拷贝数组
        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        //是否有消除
        Set<Integer> immutableElements = Set.of();
        //固定堆叠百搭符号处理
        if (lib.getLibTypeSet().contains(WolfMoonConstant.SpecialMode.FREE_FIXED_STACKED_WILD)) {
            immutableElements = freeImmutableElements;
        }
        repairIcons(newArr, lib.getAwardLineInfoList(), addIconInfoList, immutableElements);
        initAddFreeCount(lib);
        if (!addIconInfoList.isEmpty()) {
            lib.setAddIconInfos(addIconInfoList);
        }
        //计算倍数
        calTimes(lib);
        return lib;
    }

    private void initAddFreeCount(WolfMoonResultLib lib) {
        if (CollectionUtil.isNotEmpty(lib.getSpecialAuxiliaryInfoList())) {
            int againFreeCount = 0;
            int allCount = 0;
            for (SpecialAuxiliaryInfo info : lib.getSpecialAuxiliaryInfoList()) {
                if (CollectionUtil.isEmpty(info.getFreeGames())) {
                    continue;
                }
                for (JSONObject json : info.getFreeGames()) {
                    Integer addFreeCount = json.getInteger("addFreeCount");
                    if (addFreeCount != null && addFreeCount > 0) {
                        againFreeCount += addFreeCount;
                    }
                }
                allCount += info.getFreeGames().size();
            }
            //设置添加的免费次数
            lib.setAddFreeCount(allCount - againFreeCount);
        }
    }

    @Override
    public boolean autoSetFreeModelLibType() {
        return true;
    }

    @Override
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        int remainFreeCount = freeCount;
        int baseMultiple = 0;
        boolean addMultiple = specialModeType == WolfMoonConstant.SpecialMode.FREE_INCREASING_MULTIPLIER;
        if (addMultiple && freeAddCfg != null) {
            baseMultiple = freeAddCfg.getFirst();
        }
        while (remainFreeCount > 0) {
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }
            WolfMoonResultLib lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            int addCount = checkAddFreeCount(lib);
            lib.setAddFreeCount(addCount);
            lib.setBaseMultiple(baseMultiple);
            if (addMultiple) {
                //重新计算倍数
                lib.setTimes(0);
                try {
                    calTimes(lib);
                } catch (Exception e) {
                    log.error("狼月计算倍数异常", e);
                }
            }
            remainFreeCount += addCount;
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
            remainFreeCount--;
            if (addMultiple && freeAddCfg != null) {
                baseMultiple = Math.min(baseMultiple + freeAddCfg.getFirst(), freeAddCfg.getSecond());
            }
        }
    }

    /**
     * 检查是否增加免费次数
     *
     * @param lib
     * @return
     */
    private int checkAddFreeCount(WolfMoonResultLib lib) {
        if (freeIconAddCfg == null) {
            return 0;
        }
        int addCount = 0;
        for (int i = 1; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            //是否出现了目标图标
            if (icon != freeIconAddCfg.getFirst()) {
                continue;
            }
            addCount += freeIconAddCfg.getSecond();
        }
        return addCount;
    }

    @Override
    protected WolfMoonAwardLineInfo getAwardLineInfo() {
        return new WolfMoonAwardLineInfo();
    }

    /**
     * 修补图标
     */
    public void repairIcons(int[] arr, List<WolfMoonAwardLineInfo> list, List<WolfMoonAddIconInfo> addIconInfoList, Set<Integer> immutableElements) {
        if (list == null || list.isEmpty()) {
            return;
        }

        WolfMoonAddIconInfo addIconInfo = new WolfMoonAddIconInfo();

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);

        //将所有需要消除的图标进行汇总
        Map<Integer, Set<Integer>> allSameMap = new HashMap<>();
        for (WolfMoonAwardLineInfo info : list) {
            if (info.getSameIconSet() == null || info.getSameIconSet().isEmpty()) {
                continue;
            }
            info.getSameIconSet().forEach(index -> {
                if (immutableElements.contains(arr[index])) {
                    return;
                }
                int columnId = index / baseInitCfg.getRows();
                if ((index % baseInitCfg.getRows()) != 0) {
                    columnId++;
                }
                allSameMap.computeIfAbsent(columnId, k -> new HashSet<>()).add(index);
            });
        }

        //坐标对应添加的
        Map<Integer, Integer> addIconMap = new HashMap<>();

        for (Map.Entry<Integer, Set<Integer>> en : allSameMap.entrySet()) {
            int colIndex = en.getKey();
            Set<Integer> set = en.getValue();
            processIcons(colIndex, set, arr, addIconMap);
        }

        addIconInfo.setAddIconMap(addIconMap);

        //检查中奖
        List<WolfMoonAwardLineInfo> newAwardInfoList = fullLine(arr);

        addIconInfo.setAwardLineInfoList(newAwardInfoList);
        addIconInfoList.add(addIconInfo);

        repairIcons(arr, newAwardInfoList, addIconInfoList, immutableElements);
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
    public void calTimes(WolfMoonResultLib lib) throws Exception {
        //中奖线
        int baseMultiple = Math.max(1, lib.getBaseMultiple());
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList(), baseMultiple));
        //消除后新增图标
        lib.addTimes(calAfterAddIcons(lib.getAddIconInfos(), baseMultiple));
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @param baseMultiple
     * @return
     */
    public int calLineTimes(List<WolfMoonAwardLineInfo> list, int baseMultiple) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (WolfMoonAwardLineInfo awardLineInfo : list) {
            awardLineInfo.setBaseTimes(awardLineInfo.getBaseTimes() * baseMultiple);
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    /**
     * 计算消除补齐后的中奖倍数
     *
     * @param addIconInfos
     * @param baseMultiple
     * @return
     */
    public long calAfterAddIcons(List<WolfMoonAddIconInfo> addIconInfos, int baseMultiple) {
        if (addIconInfos == null || addIconInfos.isEmpty()) {
            return 0;
        }

        long times = 0;
        for (WolfMoonAddIconInfo info : addIconInfos) {
            if (info.getAwardLineInfoList() == null || info.getAwardLineInfoList().isEmpty()) {
                continue;
            }

            for (WolfMoonAwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
                awardLineInfo.setBaseTimes(awardLineInfo.getBaseTimes() * baseMultiple);
                times += awardLineInfo.getBaseTimes();
            }
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        loadFreeMultipleAdd();
        loadFreeIconAdd();
    }

    private void loadFreeMultipleAdd() {
        //1,1_10;2,2_20
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(WolfMoonConstant.SpecialPlay.FREE_MULTIPLE_ADD_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] modeArr = specialPlayCfg.getValue().split("_");
        if (modeArr.length != 2) {
            return;
        }
        freeAddCfg = Pair.newPair(Integer.parseInt(modeArr[0]), Integer.parseInt(modeArr[1]));
    }

    private void loadFreeIconAdd() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(WolfMoonConstant.SpecialPlay.FREE_ICON_ADD_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] modeArr = specialPlayCfg.getValue().split("_");
        if (modeArr.length != 2) {
            return;
        }
        freeIconAddCfg = Pair.newPair(Integer.parseInt(modeArr[0]), Integer.parseInt(modeArr[1]));
    }
}
