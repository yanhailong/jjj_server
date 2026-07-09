package com.jjg.game.slots.game.bountyduel.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseRollerCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.bountyduel.BountyDuelConstant;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelAddFreeInfo;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelAddIconInfo;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelAwardLineInfo;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 赏金大对决结果生成器。
 * <p>
 * 这个游戏客户端表现是异型棋盘，但服务端仍按 6 列 x 5 行的 30 格数组处理。
 * 中奖、消除、下落时需要跳过无效格：1、2、6、21、26、27。
 */
@Component
public class BountyDuelGenerateManager extends AbstractSlotsGenerateManager<BountyDuelAwardLineInfo, BountyDuelResultLib> {
    private static final int ROWS = 5;
    private static final int COLS = 6;
    private static final Set<Integer> INVALID_INDEXES = Set.of(1, 2, 6, 21, 26, 27);

    /**
     * 连续中奖倍数配置：libType -> 连续消除次数 -> 倍数。
     */
    private Map<Integer, Map<Integer, Integer>> addTimesMap = Map.of();
    /**
     * 连续中奖倍数配置中的最大连续消除次数。
     */
    private int maxWinCount;
    /**
     * 免费模式重触发次数配置。
     */
    private BountyDuelAddFreeInfo bountyDuelAddFreeInfo;

    public BountyDuelGenerateManager() {
        super(BountyDuelResultLib.class);
    }

    @Override
    public BountyDuelResultLib checkAward(int[] arr, BountyDuelResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);

        List<BountyDuelAwardLineInfo> fullLineInfoList = fullLine(lib);
        lib.addAllAwardLineInfo(fullLineInfoList);

        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        List<BountyDuelAddIconInfo> addIconInfoList = new ArrayList<>();

        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);

        if (lib.getLibTypeSet() != null && !lib.getLibTypeSet().isEmpty()) {
            lib.getLibTypeSet().forEach(type -> repairIcons(type, newArr, lib.getAwardLineInfoList(), addIconInfoList, 0));
        }

        if (!addIconInfoList.isEmpty()) {
            lib.setAddIconInfos(addIconInfoList);
        }

        List<JSONObject> freeGames = new ArrayList<>();
        mergeFreeResults(lib, freeGames, true);
        calTimes(lib);
        return lib;
    }

    @Override
    public SpecialAuxiliaryInfo triggerMiniGame(int specialModeType, int[] arr, int miniGameId, List<SpecialGirdInfo> specialGirdInfoList) {
        log.debug("触发小游戏 miniGameId={}", miniGameId);

        SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
        if (specialAuxiliaryCfg == null) {
            log.warn("未找到小游戏配置 miniGameId={}", miniGameId);
            return null;
        }

        SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(miniGameId);
        if (specialAuxiliaryPropConfig == null) {
            log.warn("未找到小游戏权重配置 miniGameId={}", miniGameId);
            return null;
        }

        SpecialAuxiliaryInfo specialAuxiliaryInfo = new SpecialAuxiliaryInfo();
        specialAuxiliaryInfo.setCfgId(miniGameId);

        triggerFree(arr, specialModeType, specialAuxiliaryCfg, specialAuxiliaryPropConfig, specialAuxiliaryInfo);
        triggerAuxiliaryExtra(arr, specialAuxiliaryCfg, specialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
        return specialAuxiliaryInfo;
    }

    protected void triggerFree(int[] arr, int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg,
                               SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        int[] guard = freeGenTotalGuard.get();
        boolean isRoot = (guard == null);
        if (isRoot) {
            guard = new int[]{0, 0};
            freeGenTotalGuard.set(guard);
        }

        if (guard[1] > 0) {
            freeCount = checkAddFreeCount(specialModeType, arr);
            if (freeCount < 1) {
                return;
            }
        }

        guard[1]++;
        try {
            Pair<Integer, Integer> config = getFreeGameLimitConfig();
            for (int i = 0; i < freeCount; i++) {
                if (guard[0] >= config.getFirst() || guard[1] > config.getSecond()) {
                    log.error("免费生成达到上限，跳过剩余触发 gameType={}, miniGameId={}, specialModeType={}, guard[0]={}, guard[1]={}, remain={}",
                            this.gameType, specialAuxiliaryCfg.getId(), specialModeType, guard[0], guard[1], freeCount - i);
                    break;
                }
                guard[0]++;

                int specialGroupGirdID = 0;
                if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                    Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                    if (randKey != null && randKey > 0) {
                        specialGroupGirdID = randKey;
                    }
                }

                BountyDuelResultLib t = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
                specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(t));
            }
        } finally {
            guard[1]--;
            if (isRoot) {
                freeGenTotalGuard.remove();
            }
        }
    }

    private int checkAddFreeCount(int specialModeType, int[] arr) {
        if (specialModeType != BountyDuelConstant.SpecialMode.FREE) {
            return 0;
        }
        if (this.bountyDuelAddFreeInfo == null) {
            return 0;
        }

        int times = 0;
        for (int i = 1; i < arr.length; i++) {
            if (isInvalidIndex(i)) {
                continue;
            }
            if (arr[i] == this.bountyDuelAddFreeInfo.getTargetIcon()) {
                times++;
            }
        }
        return this.bountyDuelAddFreeInfo.getAddFreeCount(times);
    }

    @Override
    public boolean autoSetFreeModelLibType() {
        return true;
    }

    @Override
    public void onMergeFreeResults(BountyDuelResultLib lib, int addCount) {
        lib.setAddFreeCount(addCount);
    }

    @Override
    protected BountyDuelAwardLineInfo addFullLineAwardInfo(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg, int[] arr) {
        BountyDuelAwardLineInfo info = getAwardLineInfo();
        info.setSameIconSet(sameIconIndexSet);
        info.setSameIcon(cfg.getElementId().getFirst());
        info.setBaseTimes(calFullLineBaseTimes(sameIconIndexSet, cfg, arr));
        return info;
    }

    private int calFullLineBaseTimes(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg, int[] arr) {
        if (sameIconIndexSet == null || sameIconIndexSet.isEmpty()) {
            return cfg.getBet();
        }

        Map<Integer, Integer> iconNum = new HashMap<>();
        Map<Integer, Integer> columnIconCountMap = new HashMap<>();
        for (int index : sameIconIndexSet) {
            int columnId = (index - 1) / ROWS + 1;
            columnIconCountMap.merge(columnId, 1, Integer::sum);
            iconNum.merge(arr[index], 1, Integer::sum);
        }

        int addTimes = getConfiguredAddTimes(cfg, iconNum);
        for (Integer count : columnIconCountMap.values()) {
            addTimes *= count;
        }
        return cfg.getBet() * addTimes;
    }

    private int getConfiguredAddTimes(BaseElementRewardCfg cfg, Map<Integer, Integer> iconNum) {
        if (cfg.getBetTimes() == null || cfg.getBetTimes().isEmpty()) {
            return 1;
        }

        for (List<Integer> betTime : cfg.getBetTimes()) {
            if (betTime.size() != 3) {
                continue;
            }

            Integer num = iconNum.get(betTime.get(0));
            if (num != null && num >= betTime.get(1)) {
                return betTime.get(2);
            }
        }
        return 1;
    }

    @Override
    protected BountyDuelAwardLineInfo getAwardLineInfo() {
        return new BountyDuelAwardLineInfo();
    }

    @Override
    public Pair<Integer, Integer> getFreeGameLimitConfig() {
        return Pair.newPair(BountyDuelConstant.Common.MAX_FREE_GAME_TOTAL, BountyDuelConstant.Common.MAX_FREE_DEEP_TOTAL);
    }

    @Override
    public List<BountyDuelAwardLineInfo> fullLine(int[] arr) {
        Map<Integer, BaseElementRewardCfg> fullLineCountCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_FULL);
        if (fullLineCountCfgMap == null || fullLineCountCfgMap.isEmpty()) {
            return null;
        }

        Set<Integer> wildIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_WILD);
        Set<Integer> normalIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_NORMAL);
        if (wildIconSet == null || normalIconSet == null) {
            return null;
        }

        Map<Integer, Set<Integer>> firstColIcons = new HashMap<>();
        for (int row = 1; row <= ROWS; row++) {
            int index = row;
            if (isInvalidIndex(index)) {
                continue;
            }

            int icon = arr[index];
            if (wildIconSet.contains(icon)) {
                for (Integer normalIcon : normalIconSet) {
                    firstColIcons.computeIfAbsent(normalIcon, k -> new HashSet<>()).add(index);
                }
            }
            firstColIcons.computeIfAbsent(icon, k -> new HashSet<>()).add(index);
        }

        List<BountyDuelAwardLineInfo> awardInfoList = new ArrayList<>();
        for (Map.Entry<Integer, Set<Integer>> en : firstColIcons.entrySet()) {
            int icon = en.getKey();
            boolean firstNormal = normalIconSet.contains(icon);

            for (Map.Entry<Integer, BaseElementRewardCfg> rewardCfgEn : fullLineCountCfgMap.entrySet()) {
                BaseElementRewardCfg cfg = rewardCfgEn.getValue();
                if (!cfg.getElementId().contains(icon)) {
                    continue;
                }

                int maxCol = 1;
                Set<Integer> sameIconIndexSet = new HashSet<>(en.getValue());
                for (int col = 2; col <= COLS; col++) {
                    int beginIndex = (col - 1) * ROWS + 1;
                    boolean flag = false;

                    for (int row = 0; row < ROWS; row++) {
                        int index = beginIndex + row;
                        if (isInvalidIndex(index)) {
                            continue;
                        }

                        int tmpIcon = arr[index];
                        boolean wild = wildIconSet.contains(tmpIcon);
                        if (wild && firstNormal) {
                            flag = true;
                            sameIconIndexSet.add(index);
                        } else if (cfg.getElementId().contains(tmpIcon)) {
                            flag = true;
                            sameIconIndexSet.add(index);
                        }
                    }

                    if (!flag) {
                        break;
                    }
                    maxCol = col;
                }

                if (maxCol == cfg.getRewardNum()) {
                    BountyDuelAwardLineInfo rewardInfo = addFullLineAwardInfo(sameIconIndexSet, cfg, arr);
                    awardInfoList.add(rewardInfo);
                }
            }
        }
        return awardInfoList;
    }

    public void repairIcons(int libType, int[] arr, List<BountyDuelAwardLineInfo> list,
                            List<BountyDuelAddIconInfo> addIconInfoList, int winCount) {
        if (list == null || list.isEmpty()) {
            return;
        }

        winCount++;
        resetLineRewardTimes(libType, winCount, list);

        BountyDuelAddIconInfo addIconInfo = new BountyDuelAddIconInfo();
        Map<Integer, Set<Integer>> allSameMap = new HashMap<>();

        for (BountyDuelAwardLineInfo info : list) {
            if (info.getSameIconSet() == null || info.getSameIconSet().isEmpty()) {
                continue;
            }

            Set<Integer> replaceWildIndexs = new HashSet<>();
            info.getSameIconSet().forEach(index -> {
                if (isInvalidIndex(index)) {
                    return;
                }

                int columnId = index / ROWS;
                if ((index % ROWS) != 0) {
                    columnId++;
                }
                allSameMap.computeIfAbsent(columnId, k -> new HashSet<>()).add(index);

                int icon = arr[index];
                if (icon >= BountyDuelConstant.BaseElement.GOLD_MIN && icon <= BountyDuelConstant.BaseElement.GOLD_MAX) {
                    Integer replaceIcon = getPostChangeIcon(icon);
                    if (replaceIcon != null) {
                        replaceWildIndexs.add(index);
                    }
                }
            });

            info.setReplaceWildIndexs(replaceWildIndexs);
        }

        Map<Integer, Integer> addIconMap = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> en : allSameMap.entrySet()) {
            processIcons(en.getKey(), en.getValue(), arr, addIconMap);
        }

        addIconInfo.setAddIconMap(addIconMap);
        List<BountyDuelAwardLineInfo> newAwardInfoList = fullLine(arr);
        addIconInfo.setAwardLineInfoList(newAwardInfoList);
        addIconInfoList.add(addIconInfo);

        repairIcons(libType, arr, newAwardInfoList, addIconInfoList, winCount);
    }

    private void resetLineRewardTimes(int libType, int winCount, List<BountyDuelAwardLineInfo> list) {
        Map<Integer, Integer> temMap = this.addTimesMap.get(libType);
        if (temMap == null || temMap.isEmpty()) {
            return;
        }

        Integer times = winCount > this.maxWinCount ? temMap.get(this.maxWinCount) : temMap.get(winCount);
        if (times == null) {
            return;
        }

        list.forEach(info -> info.setBaseTimes(info.getBaseTimes() * times));
    }

    /**
     * 处理指定列的消除、下落和补图。异型无效格不参与移动和补图。
     */
    public void processIcons(int colIndex, Set<Integer> removedIndexes, int[] arr, Map<Integer, Integer> addIconMap) {
        int beginIndex = (colIndex - 1) * ROWS + 1;
        int endIndex = beginIndex + ROWS - 1;

        List<Integer> validIndexes = new ArrayList<>(ROWS - removedIndexes.size());
        for (int i = beginIndex; i <= endIndex; i++) {
            if (isInvalidIndex(i)) {
                continue;
            }

            int icon = arr[i];
            if (removedIndexes.contains(i)) {
                if (icon >= BountyDuelConstant.BaseElement.GOLD_MIN && icon <= BountyDuelConstant.BaseElement.GOLD_MAX) {
                    Integer replaceIcon = getPostChangeIcon(icon);
                    if (replaceIcon != null) {
                        validIndexes.add(replaceIcon);
                    }
                }
            } else {
                validIndexes.add(icon);
            }
            arr[i] = -1;
        }

        validIndexes = validIndexes.reversed();
        int curIndex = endIndex;
        for (Integer validIndex : validIndexes) {
            while (curIndex >= beginIndex && isInvalidIndex(curIndex)) {
                curIndex--;
            }
            if (curIndex < beginIndex) {
                break;
            }
            arr[curIndex] = validIndex;
            curIndex--;
        }

        Map<Integer, BaseRollerCfg> rollerCfgMap = this.baseRollerCfgMap.entrySet().stream().findFirst().get().getValue();
        BaseRollerCfg baseRollerCfg = rollerCfgMap.get(colIndex);

        int first = baseRollerCfg.getAxleCountScope().get(0) - 1;
        int last = baseRollerCfg.getAxleCountScope().get(1) - 1;
        int scopeIndex = RandomUtils.randomMinMax(first, last);

        for (int i = 0; i < ROWS; i++) {
            if (scopeIndex > last) {
                scopeIndex = first;
            }

            int index = beginIndex + i;
            if (isInvalidIndex(index) || arr[index] > 0) {
                continue;
            }

            int elementId = baseRollerCfg.getElements().get(scopeIndex);
            arr[index] = elementId;
            addIconMap.put(index, elementId);
            log.debug("补充新图标 index={}, icon={}", index, elementId);

            scopeIndex++;
        }
    }

    private boolean isInvalidIndex(int index) {
        return INVALID_INDEXES.contains(index);
    }

    @Override
    public void calTimes(BountyDuelResultLib lib) throws Exception {
        if (triggerFreeLib(lib, BountyDuelConstant.SpecialMode.FREE)) {
            lib.addTimes(calFree(lib));
        } else {
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
            lib.addTimes(calAfterAddIcons(lib.getAddIconInfos()));
        }
    }

    public int calLineTimes(List<BountyDuelAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (BountyDuelAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    public long calAfterAddIcons(List<BountyDuelAddIconInfo> addIconInfos) {
        if (addIconInfos == null || addIconInfos.isEmpty()) {
            return 0;
        }

        long times = 0;
        for (BountyDuelAddIconInfo info : addIconInfos) {
            if (info.getAwardLineInfoList() == null || info.getAwardLineInfoList().isEmpty()) {
                continue;
            }
            for (BountyDuelAwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
                times += awardLineInfo.getBaseTimes();
            }
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        Map<Integer, Map<Integer, Integer>> tmpAddTimesMap = new HashMap<>();

        int tmpMaxWinCount = 0;
        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            if (cfg.getPlayType() == BountyDuelConstant.SpecialPlay.TYPE_CONSECUTIVE_WINS) {
                String[] arr = cfg.getValue().split(";");
                for (String s : arr) {
                    String[] arr1 = s.split(",");
                    int libType = Integer.parseInt(arr1[0]);
                    Map<Integer, Integer> temMap = tmpAddTimesMap.computeIfAbsent(libType, k -> new HashMap<>());
                    String[] arr2 = arr1[1].split("\\|");
                    for (String s2 : arr2) {
                        String[] arr3 = s2.split("_");
                        int count = Integer.parseInt(arr3[0]);
                        int times = Integer.parseInt(arr3[1]);
                        temMap.put(count, times);
                        if (count > tmpMaxWinCount) {
                            tmpMaxWinCount = count;
                        }
                    }
                }
            } else if (cfg.getPlayType() == BountyDuelConstant.SpecialPlay.TYPE_ADD_FREE_COUNT) {
                BountyDuelAddFreeInfo tmpBountyDuelAddFreeInfo = new BountyDuelAddFreeInfo();
                String[] arr0 = cfg.getValue().split(",");

                tmpBountyDuelAddFreeInfo.setLibType(BountyDuelConstant.SpecialMode.FREE);
                tmpBountyDuelAddFreeInfo.setTargetIcon(Integer.parseInt(arr0[0]));

                String[] arr1 = arr0[1].split("\\|");
                for (String addFreeInfoStr : arr1) {
                    String[] arr2 = addFreeInfoStr.split("_");

                    int times = Integer.parseInt(arr2[0]);
                    int addFreeCount = Integer.parseInt(arr2[1]);
                    int prop = Integer.parseInt(arr2[2]);

                    tmpBountyDuelAddFreeInfo.addTimesInfo(times, addFreeCount, prop);
                }

                this.bountyDuelAddFreeInfo = tmpBountyDuelAddFreeInfo;
            }
        }
        this.addTimesMap = tmpAddTimesMap;
        this.maxWinCount = tmpMaxWinCount;
    }

    public Map<Integer, Map<Integer, Integer>> getAddTimesMap() {
        return addTimesMap;
    }
}
