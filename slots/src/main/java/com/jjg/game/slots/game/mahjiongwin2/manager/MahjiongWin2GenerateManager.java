package com.jjg.game.slots.game.mahjiongwin2.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.mahjiongwin2.MahjiongWin2Constant;
import com.jjg.game.slots.game.mahjiongwin2.data.MahjiongWin2AddFreeInfo;
import com.jjg.game.slots.game.mahjiongwin2.data.MahjiongWin2AddIconInfo;
import com.jjg.game.slots.game.mahjiongwin2.data.MahjiongWin2AwardLineInfo;
import com.jjg.game.slots.game.mahjiongwin2.data.MahjiongWin2ResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2025/8/1 17:33
 */
@Component
public class MahjiongWin2GenerateManager extends AbstractSlotsGenerateManager<MahjiongWin2AwardLineInfo, MahjiongWin2ResultLib> {
    //连续中奖增加倍数  libType -> count -> times
    private Map<Integer, Map<Integer, Integer>> addTimesMap = Map.of();
    //连续中奖增加倍数时，最大连续中奖次数
    private int maxWinCount;
    private MahjiongWin2AddFreeInfo mahjiongWinAddFreeInfo;

    public MahjiongWin2GenerateManager() {
        super(MahjiongWin2ResultLib.class);
    }

    @Override
    public MahjiongWin2ResultLib checkAward(int[] arr, MahjiongWin2ResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);

        //检查满线图案
        List<MahjiongWin2AwardLineInfo> fullLineInfoList = fullLine(lib);
        lib.addAllAwardLineInfo(fullLineInfoList);

        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        //存储消除后添加的图标
        List<MahjiongWin2AddIconInfo> addIconInfoList = new ArrayList<>();

        //拷贝数组
        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);

        if (lib.getLibTypeSet() != null && !lib.getLibTypeSet().isEmpty()) {
            lib.getLibTypeSet().forEach(type -> {
                //是否有消除
                repairIcons(type, newArr, lib.getAwardLineInfoList(), addIconInfoList, 0);
            });
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
        triggerFree(arr, specialModeType, specialAuxiliaryCfg, specialAuxiliaryPropConfig, specialAuxiliaryInfo);
        //检查是否有额外奖励
        triggerAuxiliaryExtra(arr, specialAuxiliaryCfg, specialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
        return specialAuxiliaryInfo;
    }

    protected void triggerFree(int[] arr, int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        //防止嵌套触发免费时总局数无限膨胀导致内存溢出。同一根 checkAward 调用链共享一个累计计数器
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
                    log.error("免费生成达到硬上限，跳过剩余触发 gameType={},miniGameId={},specialModeType={},guard[0]={},guard[1]={},剩余请求={}", this.gameType, specialAuxiliaryCfg.getId(), specialModeType, guard[0], guard[1], freeCount - i);
                    break;
                }
                guard[0]++;

                //检查是否有修改图案策略组id
                int specialGroupGirdID = 0;
                if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                    Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                    if (randKey != null && randKey > 0) {
                        specialGroupGirdID = randKey;
                    }
                }

                MahjiongWin2ResultLib t = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
                specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(t));
            }
        } finally {
            guard[1]--;
            if (isRoot) {
                freeGenTotalGuard.remove();
            }
        }
    }

    /**
     * 检查是否增加免费次数
     *
     * @return
     */
    private int checkAddFreeCount(int specialModeType, int[] arr) {
        if (specialModeType != MahjiongWin2Constant.SpecialMode.FREE) {
            return 0;
        }
        int times = 0;
        for (int i = 1; i < arr.length; i++) {
            int icon = arr[i];
            //是否出现了目标图标
            if (icon != this.mahjiongWinAddFreeInfo.getTargetIcon()) {
                continue;
            }
            times++;
        }

        return this.mahjiongWinAddFreeInfo.getAddFreeCount(times);
    }

    @Override
    public boolean autoSetFreeModelLibType() {
        return true;
    }

    @Override
    public void onMergeFreeResults(MahjiongWin2ResultLib lib, int addCount) {
        lib.setAddFreeCount(addCount);
    }

    @Override
    protected MahjiongWin2AwardLineInfo addFullLineAwardInfo(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg, int[] arr) {
        MahjiongWin2AwardLineInfo info = super.addFullLineAwardInfo(sameIconIndexSet, cfg, arr);
        info.setSameIcon(cfg.getElementId().getFirst());
        return info;
    }

    @Override
    protected MahjiongWin2AwardLineInfo getAwardLineInfo() {
        return new MahjiongWin2AwardLineInfo();
    }

    @Override
    public Pair<Integer, Integer> getFreeGameLimitConfig() {
        return Pair.newPair(MahjiongWin2Constant.Common.MAX_FREE_GAME_TOTAL, MahjiongWin2Constant.Common.MAX_FREE_DEEP_TOTAL);
    }

    @Override
    public List<MahjiongWin2AwardLineInfo> fullLine(int[] arr) {
        //拷贝数组
        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        newArr[1] = SlotsConst.Common.IMMUTABLE_ELEMENTS;
        newArr[21] = SlotsConst.Common.IMMUTABLE_ELEMENTS;
        return super.fullLine(newArr);
    }

    /**
     * 修补图标
     */
    public void repairIcons(int libType, int[] arr, List<MahjiongWin2AwardLineInfo> list, List<MahjiongWin2AddIconInfo> addIconInfoList, int winCount) {
        if (list == null || list.isEmpty()) {
            return;
        }

        winCount++;

        //连续中奖后重置中奖倍数
        resetLineRewardTimes(libType, winCount, list);

        MahjiongWin2AddIconInfo addIconInfo = new MahjiongWin2AddIconInfo();

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);

        //将所有需要消除的图标进行汇总
        Map<Integer, Set<Integer>> allSameMap = new HashMap<>();
        for (MahjiongWin2AwardLineInfo info : list) {
            if (info.getSameIconSet() == null || info.getSameIconSet().isEmpty()) {
                continue;
            }

            //替换成wild的坐标
            Set<Integer> replaceWildIndexs = new HashSet<>();

            info.getSameIconSet().forEach(index -> {
                int columnId = index / baseInitCfg.getRows();
                if ((index % baseInitCfg.getRows()) != 0) {
                    columnId++;
                }
                allSameMap.computeIfAbsent(columnId, k -> new HashSet<>()).add(index);

                int icon = arr[index];

                //判断消除的图标是不是金色图标
                if (icon >= MahjiongWin2Constant.BaseElement.GOLD_MIN && icon <= MahjiongWin2Constant.BaseElement.GOLD_MAX) {
                    Integer replaceIcon = getPostChangeIcon(icon);
                    if (replaceIcon != null) {
                        replaceWildIndexs.add(index);
                    }
                }
            });

            info.setReplaceWildIndexs(replaceWildIndexs);
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
        List<MahjiongWin2AwardLineInfo> newAwardInfoList = fullLine(arr);

        addIconInfo.setAwardLineInfoList(newAwardInfoList);
        addIconInfoList.add(addIconInfo);

        repairIcons(libType, arr, newAwardInfoList, addIconInfoList, winCount);
    }

    private void resetLineRewardTimes(int libType, int winCount, List<MahjiongWin2AwardLineInfo> list) {
        Map<Integer, Integer> temMap = this.addTimesMap.get(libType);
        if (temMap == null || temMap.isEmpty()) {
            return;
        }

        Integer times;
        if (winCount > this.maxWinCount) {
            times = temMap.get(this.maxWinCount);
        } else {
            times = temMap.get(winCount);
        }

        if (times == null) {
            return;
        }

        list.forEach(info -> {
            info.setBaseTimes(info.getBaseTimes() * times);
        });
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


        //这一列开始坐标
        int beginIndex = (colIndex - 1) * rows + 1;
        //这一列结束坐标
        int endIndex = beginIndex + rows - 1;

        //找到这一列，消除后应该剩余的图标
        List<Integer> validIndexes = new ArrayList<>(baseInitCfg.getRows() - removedIndexes.size());
        for (int i = beginIndex; i <= endIndex; i++) {
            int icon = arr[i];
            if (removedIndexes.contains(i)) {
                //判断消除的图标是不是金色图标
                if (icon >= MahjiongWin2Constant.BaseElement.GOLD_MIN && icon <= MahjiongWin2Constant.BaseElement.GOLD_MAX) {
                    Integer replaceIcon = getPostChangeIcon(icon);
                    if (replaceIcon != null) {
                        validIndexes.add(replaceIcon);
                    }
                }
            } else {
                //pos1(第一列顶) / pos21(第五列顶) 的特殊图标存活后，下落时显示为 oldArr 中对应位置的原始图标
                if (i == 1 || i == 21) {
                    validIndexes.add(arr[i]);
                } else {
                    validIndexes.add(icon);
                }
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

        Map<Integer, BaseRollerCfg> rollerCfgMap = this.baseRollerCfgMap.entrySet().stream().findFirst().get().getValue();
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
    public void calTimes(MahjiongWin2ResultLib lib) throws Exception {
        if (triggerFreeLib(lib, MahjiongWin2Constant.SpecialMode.FREE)) {
            //免费
            lib.addTimes(calFree(lib));
        } else {
            //中奖线
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
            //消除后新增图标
            lib.addTimes(calAfterAddIcons(lib.getAddIconInfos()));
        }
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<MahjiongWin2AwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (MahjiongWin2AwardLineInfo awardLineInfo : list) {
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
    public long calAfterAddIcons(List<MahjiongWin2AddIconInfo> addIconInfos) {
        if (addIconInfos == null || addIconInfos.isEmpty()) {
            return 0;
        }

        long times = 0;
        for (MahjiongWin2AddIconInfo info : addIconInfos) {
            if (info.getAwardLineInfoList() == null || info.getAwardLineInfoList().isEmpty()) {
                continue;
            }

            for (MahjiongWin2AwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
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

            //连续中奖
            if (cfg.getPlayType() == MahjiongWin2Constant.SpecialPlay.TYPE_CONSECUTIVE_WINS) {
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
            } else if (cfg.getPlayType() == MahjiongWin2Constant.SpecialPlay.TYPE_ADD_FREE_COUNT) {  //增加免费次数
                MahjiongWin2AddFreeInfo tmpMahjiongWinAddFreeInfo = new MahjiongWin2AddFreeInfo();
                String[] arr0 = cfg.getValue().split(",");

                tmpMahjiongWinAddFreeInfo.setLibType(Integer.parseInt(arr0[0]));
                tmpMahjiongWinAddFreeInfo.setTargetIcon(Integer.parseInt(arr0[1]));

                String[] arr1 = arr0[2].split("\\|");
                for (String frozenThroneAddFreeInfoStr : arr1) {
                    String[] arr2 = frozenThroneAddFreeInfoStr.split("_");

                    int times = Integer.parseInt(arr2[0]);
                    int addFreeCount = Integer.parseInt(arr2[1]);
                    int prop = Integer.parseInt(arr2[2]);

                    tmpMahjiongWinAddFreeInfo.addTimesInfo(times, addFreeCount, prop);
                }

                this.mahjiongWinAddFreeInfo = tmpMahjiongWinAddFreeInfo;
            }
        }
        this.addTimesMap = tmpAddTimesMap;
        this.maxWinCount = tmpMaxWinCount;
    }

    public Map<Integer, Map<Integer, Integer>> getAddTimesMap() {
        return addTimesMap;
    }
}
