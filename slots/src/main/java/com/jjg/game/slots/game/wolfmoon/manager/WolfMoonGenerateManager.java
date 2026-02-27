package com.jjg.game.slots.game.wolfmoon.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonAddIconInfo;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonAwardLineInfo;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WolfMoonGenerateManager extends AbstractSlotsGenerateManager<WolfMoonAwardLineInfo, WolfMoonResultLib> {
    private int freeMultiplierStart = 5;
    private int freeMultiplierStep = 5;
    private int freeMultiplierMax = 100;

    public WolfMoonGenerateManager() {
        super(WolfMoonResultLib.class);
    }

    @Override
    public WolfMoonResultLib generateFreeOne(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, int specialGroupGirdID) {
        WolfMoonResultLib lib = super.generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
        if (lib != null) {
            lib.addLibType(specialModeType);
        }
        return lib;
    }

    @Override
    public WolfMoonResultLib checkAward(int[] arr, WolfMoonResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);

        List<WolfMoonAwardLineInfo> fullLineInfoList = fullLine(lib);
        applyWildBonus(arr, fullLineInfoList, lib);
        lib.addAllAwardLineInfo(fullLineInfoList);

        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        List<WolfMoonAddIconInfo> addIconInfoList = new ArrayList<>();
        int[] newArr = Arrays.copyOf(arr, arr.length);
        repairIcons(lib, newArr, lib.getAwardLineInfoList(), addIconInfoList);

        if (!addIconInfoList.isEmpty()) {
            lib.setAddIconInfos(addIconInfoList);
        }

        calTimes(lib);
        return lib;
    }

    @Override
    protected WolfMoonAwardLineInfo getAwardLineInfo() {
        return new WolfMoonAwardLineInfo();
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(WolfMoonResultLib lib) {
        Map<Integer, BaseElementRewardCfg> cfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (cfgMap == null || cfgMap.isEmpty()) {
            return null;
        }

        Map<Integer, Integer> showCountMap = checkIconShowCount(lib.getIconArr());
        Map<Integer, Set<Integer>> showIndexMap = checkIconShowIndex(lib.getIconArr());

        Set<Integer> showAuxiliaryIdSet = new HashSet<>();
        addShowAuxiliaryId(lib, showAuxiliaryIdSet);

        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = new ArrayList<>();
        for (BaseElementRewardCfg cfg : cfgMap.values()) {
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

            if (cfg.getElementId().contains(WolfMoonConstant.BaseElement.ID_SCATTER)) {
                WolfMoonAwardLineInfo scatterAward = new WolfMoonAwardLineInfo();
                scatterAward.setBaseTimes(cfg.getBet());
                scatterAward.setSameIcon(WolfMoonConstant.BaseElement.ID_SCATTER);
                scatterAward.setSameIconSet(showIndexMap.getOrDefault(WolfMoonConstant.BaseElement.ID_SCATTER, Collections.emptySet()));
                lib.addAwardLineInfo(scatterAward);
            }

            if (cfg.getFeatureTriggerId() != null && !cfg.getFeatureTriggerId().isEmpty()) {
                for (int miniGameId : cfg.getFeatureTriggerId()) {
                    if (showAuxiliaryIdSet.contains(miniGameId)) {
                        continue;
                    }
                    if (lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
                        continue;
                    }
                    for (int libType : lib.getLibTypeSet()) {
                        SpecialAuxiliaryInfo info = triggerMiniGame(libType, lib.getIconArr(), miniGameId, lib.getSpecialGirdInfoList());
                        if (info != null) {
                            showAuxiliaryIdSet.add(miniGameId);
                            specialAuxiliaryInfoList.add(info);
                        }
                    }
                }
            }

            if (cfg.getJackpotID() > 0 && (lib.getJackpotIds() == null || lib.getJackpotIds().isEmpty())) {
                lib.addJackpotId(cfg.getJackpotID());
            }
        }
        return specialAuxiliaryInfoList;
    }

    @Override
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg,
                               SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig,
                               SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        int remainFreeCount = freeCount;
        while (remainFreeCount > 0) {
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }

            WolfMoonResultLib freeSpinLib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            if (freeSpinLib == null) {
                break;
            }
            int addCount = checkAddFreeCount(freeSpinLib);
            freeSpinLib.setAddFreeCount(addCount);

            remainFreeCount += addCount;
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(freeSpinLib));
            remainFreeCount--;
        }
    }

    private int checkAddFreeCount(WolfMoonResultLib lib) {
        if (lib.getIconArr() == null || lib.getIconArr().length < 2) {
            return 0;
        }

        int addCount = 0;
        for (int i = 1; i < lib.getIconArr().length; i++) {
            if (lib.getIconArr()[i] == WolfMoonConstant.BaseElement.ID_ADD_FREE) {
                addCount++;
            }
        }
        return addCount;
    }

    private void repairIcons(WolfMoonResultLib lib, int[] arr, List<WolfMoonAwardLineInfo> awardInfos, List<WolfMoonAddIconInfo> addIconInfoList) {
        if (awardInfos == null || awardInfos.isEmpty()) {
            return;
        }

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        int rows = baseInitCfg.getRows();

        Map<Integer, Set<Integer>> removeByColMap = new HashMap<>();
        for (WolfMoonAwardLineInfo info : awardInfos) {
            if (info.getSameIconSet() == null || info.getSameIconSet().isEmpty()) {
                continue;
            }

            Set<Integer> removeIndexes = new HashSet<>(info.getSameIconSet());
            if (isStickyWildFree(lib)) {
                removeIndexes.removeIf(index -> arr[index] == WolfMoonConstant.BaseElement.ID_WILD);
            }
            if (removeIndexes.isEmpty()) {
                continue;
            }

            for (Integer index : removeIndexes) {
                int colId = index / rows;
                if ((index % rows) != 0) {
                    colId++;
                }
                removeByColMap.computeIfAbsent(colId, k -> new HashSet<>()).add(index);
            }
        }

        if (removeByColMap.isEmpty()) {
            return;
        }

        int rollerMode = lib.getRollerMode() > 0 ? lib.getRollerMode() : this.baseRollerCfgMap.keySet().stream().findFirst().orElse(0);
        Map<Integer, Integer> addIconMap = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : removeByColMap.entrySet()) {
            processIcons(rollerMode, entry.getKey(), entry.getValue(), arr, addIconMap);
        }

        List<WolfMoonAwardLineInfo> nextAwardInfos = fullLine(arr);
        applyWildBonus(arr, nextAwardInfos, lib);

        WolfMoonAddIconInfo addIconInfo = new WolfMoonAddIconInfo();
        addIconInfo.setAddIconMap(addIconMap);
        addIconInfo.setAwardLineInfoList(nextAwardInfos);
        addIconInfoList.add(addIconInfo);

        repairIcons(lib, arr, nextAwardInfos, addIconInfoList);
    }

    private void processIcons(int rollerMode, int colIndex, Set<Integer> removedIndexes, int[] arr, Map<Integer, Integer> addIconMap) {
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        int rows = baseInitCfg.getRows();

        int beginIndex = (colIndex - 1) * rows + 1;
        int endIndex = beginIndex + rows - 1;

        List<Integer> remainIcons = new ArrayList<>(rows - removedIndexes.size());
        for (int i = beginIndex; i <= endIndex; i++) {
            int icon = arr[i];
            if (removedIndexes.contains(i)) {
                Integer replace = getPostChangeIcon(icon);
                if (replace != null) {
                    remainIcons.add(replace);
                }
            } else {
                remainIcons.add(icon);
            }
            arr[i] = -1;
        }

        Collections.reverse(remainIcons);
        int curIndex = endIndex;
        for (int icon : remainIcons) {
            arr[curIndex--] = icon;
        }

        Map<Integer, BaseRollerCfg> rollerCfgMap = this.baseRollerCfgMap.get(rollerMode);
        if (rollerCfgMap == null || rollerCfgMap.isEmpty()) {
            return;
        }
        BaseRollerCfg baseRollerCfg = rollerCfgMap.get(colIndex);
        if (baseRollerCfg == null || baseRollerCfg.getAxleCountScope() == null || baseRollerCfg.getAxleCountScope().size() < 2) {
            return;
        }

        int first = baseRollerCfg.getAxleCountScope().get(0) - 1;
        int last = baseRollerCfg.getAxleCountScope().get(1) - 1;
        int scopeIndex = RandomUtils.randomMinMax(first, last);

        for (int i = 0; i < rows; i++) {
            if (scopeIndex > last) {
                scopeIndex = first;
            }
            int index = beginIndex + i;
            if (arr[index] > 0) {
                continue;
            }
            int elementId = baseRollerCfg.getElements().get(scopeIndex);
            arr[index] = elementId;
            addIconMap.put(index, elementId);
            scopeIndex++;
        }
    }

    private void applyWildBonus(int[] arr, List<WolfMoonAwardLineInfo> awardInfos, WolfMoonResultLib lib) {
        if (awardInfos == null || awardInfos.isEmpty()) {
            return;
        }
        if (isWishWildMode(lib)) {
            return;
        }

        for (WolfMoonAwardLineInfo info : awardInfos) {
            if (info.getSameIconSet() == null || info.getSameIconSet().isEmpty()) {
                continue;
            }

            boolean containsWild = false;
            for (Integer index : info.getSameIconSet()) {
                if (arr[index] == WolfMoonConstant.BaseElement.ID_WILD) {
                    containsWild = true;
                    break;
                }
            }
            info.setContainsWild(containsWild);
            if (containsWild) {
                info.setBaseTimes(info.getBaseTimes() * 2);
            }
        }
    }

    private boolean isWishWildMode(WolfMoonResultLib lib) {
        return lib != null
                && lib.getLibTypeSet() != null
                && lib.getLibTypeSet().contains(WolfMoonConstant.SpecialMode.WISH_WILD);
    }

    private boolean isStickyWildFree(WolfMoonResultLib lib) {
        return lib != null
                && lib.getLibTypeSet() != null
                && lib.getLibTypeSet().contains(WolfMoonConstant.SpecialMode.FREE_STACK_WILD);
    }

    @Override
    public void calTimes(WolfMoonResultLib lib) throws Exception {
        if (triggerFreeLib(lib)) {
            lib.setTimes(calFree(lib));
            return;
        }

        long times = calLineTimes(lib.getAwardLineInfoList());
        times += calAfterAddIcons(lib.getAddIconInfos());
        lib.setTimes(times);
    }

    public int calLineTimes(List<WolfMoonAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (WolfMoonAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    public long calAfterAddIcons(List<WolfMoonAddIconInfo> addIconInfos) {
        if (addIconInfos == null || addIconInfos.isEmpty()) {
            return 0;
        }

        long times = 0;
        for (WolfMoonAddIconInfo info : addIconInfos) {
            if (info.getAwardLineInfoList() == null || info.getAwardLineInfoList().isEmpty()) {
                continue;
            }
            for (WolfMoonAwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
                times += awardLineInfo.getBaseTimes();
            }
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            if (cfg.getPlayType() == WolfMoonConstant.SpecialPlay.TYPE_FREE_MULTIPLIER) {
                String[] arr = cfg.getValue().split("_");
                if (arr.length >= 2) {
                    this.freeMultiplierStart = Integer.parseInt(arr[1]);
                    this.freeMultiplierStep = Integer.parseInt(arr[1]);
                }
            }
        }
    }

    public int getFreeMultiplierStart() {
        return freeMultiplierStart;
    }

    public int getFreeMultiplierStep() {
        return freeMultiplierStep;
    }

    public int getFreeMultiplierMax() {
        return freeMultiplierMax;
    }

    private void addShowAuxiliaryId(WolfMoonResultLib lib, Set<Integer> set) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return;
        }

        lib.getSpecialAuxiliaryInfoList().forEach(info -> set.add(info.getCfgId()));
    }
}
