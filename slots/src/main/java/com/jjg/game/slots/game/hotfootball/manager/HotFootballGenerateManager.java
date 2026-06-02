package com.jjg.game.slots.game.hotfootball.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.data.PropInfo;
import com.jjg.game.core.utils.PropUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.game.hotfootball.HotFootballConstant;
import com.jjg.game.slots.game.hotfootball.data.HotFootballAddFreeInfo;
import com.jjg.game.slots.game.hotfootball.data.HotFootballAddIconInfo;
import com.jjg.game.slots.game.hotfootball.data.HotFootballAwardLineInfo;
import com.jjg.game.slots.game.hotfootball.data.HotFootballResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2025/8/1 17:33
 */
@Component
public class HotFootballGenerateManager extends AbstractSlotsGenerateManager<HotFootballAwardLineInfo, HotFootballResultLib> {
    public HotFootballGenerateManager() {
        super(HotFootballResultLib.class);
    }

    //连续中奖增加倍数  libType -> count -> times
    private Map<Integer, Map<Integer, Integer>> addTimesMap;
    //连续中奖增加倍数时，最大连续中奖次数
    private int maxWinCount;
    //
    private HotFootballAddFreeInfo hotFootballAddFreeInfo;

    private Map<Integer, BaseElementCfg> baseElementCfgMap;

    //免费模式能量值/守门员状态（文档 [37-43]）
    //state[0]=multiplier 当前乘倍值, state[1]=maxEnergy 当前能量满值, state[2]=energy 当前能量
    //在 triggerFree 一次会话期内 sticky，跨多个免费 spin 累加
    private static final ThreadLocal<int[]> energyMeterTL = new ThreadLocal<>();

    //能量值机制常量
    private static final int ENERGY_INITIAL_MULTIPLIER = 2;
    private static final int ENERGY_INITIAL_MAX = 6;
    private static final int ENERGY_MAX_CAP = 16;
    private static final int ENERGY_STEP = 2;
    private static final int MULTIPLIER_STEP = 2;

    @Override
    public HotFootballResultLib checkAward(int[] arr, HotFootballResultLib lib, boolean freeModel) throws Exception {
        if(freeModel){
            lib.setGameType(this.gameType);
            lib.setIconArr(arr);

            //检查满线图案
            List<HotFootballAwardLineInfo> fullLineInfoList = fullLine(lib);
            lib.addAllAwardLineInfo(fullLineInfoList);

            //检查全局分散图案
            List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
            lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

            //存储消除后添加的图标
            List<HotFootballAddIconInfo> addIconInfoList = new ArrayList<>();

            //拷贝数组
            int[] newArr = new int[arr.length];
            System.arraycopy(arr, 0, newArr, 0, arr.length);

            //是否有消除
            repairIcons(HotFootballConstant.SpecialMode.FREE, newArr, lib.getAwardLineInfoList(), addIconInfoList, 0);

            if (!addIconInfoList.isEmpty()) {
                lib.setAddIconInfos(addIconInfoList);
            }

            calTimes(lib);
            return lib;
        }else {
            lib.setGameType(this.gameType);
            lib.setIconArr(arr);

            //检查满线图案
            List<HotFootballAwardLineInfo> fullLineInfoList = fullLine(lib);
            lib.addAllAwardLineInfo(fullLineInfoList);

            //检查全局分散图案
            List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
            lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

            //存储消除后添加的图标
            List<HotFootballAddIconInfo> addIconInfoList = new ArrayList<>();

            //拷贝数组
            int[] newArr = new int[arr.length];
            System.arraycopy(arr, 0, newArr, 0, arr.length);

            if(lib.getLibTypeSet() != null && !lib.getLibTypeSet().isEmpty()) {
                lib.getLibTypeSet().forEach(type -> {
                    //是否有消除
                    repairIcons(type, newArr, lib.getAwardLineInfoList(), addIconInfoList, 0);
                });
            }

            if (!addIconInfoList.isEmpty()) {
                lib.setAddIconInfos(addIconInfoList);
            }

            calTimes(lib);
            return lib;
        }
    }
    @Override
    protected HotFootballAwardLineInfo addFullLineAwardInfo(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg, int[] arr) {
        HotFootballAwardLineInfo info = super.addFullLineAwardInfo(sameIconIndexSet, cfg, arr);
        info.setSameIcon(cfg.getElementId().getFirst() % 10);
        return info;
    }

    @Override
    protected HotFootballAwardLineInfo getAwardLineInfo() {
        return new HotFootballAwardLineInfo();
    }

    @Override
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg,
                               SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        log.debug("增加免费游戏次数 addCount = {}", freeCount);

        int remainFreeCount = freeCount;

        //防止嵌套触发免费时总局数无限膨胀导致内存溢出。同一根 checkAward 调用链共享一个累计计数器
        int[] guard = freeGenTotalGuard.get();
        boolean isRoot = (guard == null);
        if (isRoot) {
            guard = new int[]{0, 0};
            freeGenTotalGuard.set(guard);

            //免费游戏会话开始：初始化能量值/守门员状态（multiplier=2, maxEnergy=6, energy=0）
            energyMeterTL.set(new int[]{ENERGY_INITIAL_MULTIPLIER, ENERGY_INITIAL_MAX, 0});
        }

        guard[1]++;
        try {
            while (remainFreeCount > 0) {
                if (guard[0] >= SlotsConst.Common.MAX_FREE_GAME_TOTAL || guard[1] > SlotsConst.Common.MAX_FREE_DEEP_TOTAL) {
                    log.error("免费生成达到硬上限，跳过剩余触发 gameType={},miniGameId={},specialModeType={},guard[0]={},guard[1]={},剩余请求={}", this.gameType, specialAuxiliaryCfg.getId(), specialModeType, guard[0], guard[1], remainFreeCount);
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

                HotFootballResultLib lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);

                //捕获本局结束后的能量值/守门员状态写到 lib 给客户端显示
                int[] state = energyMeterTL.get();
                if (state != null && lib != null) {
                    lib.setMultiplier(state[0]);
                    lib.setMaxEnergyAfter(state[1]);
                    lib.setEnergyAfter(state[2]);
                }

                int addCount = checkAddFreeCount(lib);
                lib.setAddFreeCount(addCount);
                remainFreeCount += addCount;
                specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
                remainFreeCount--;
            }
        } finally {
            guard[1]--;
            if (isRoot) {
                freeGenTotalGuard.remove();
                energyMeterTL.remove();
            }
        }
    }

    /**
     * 检查是否增加免费次数
     *
     * @param lib
     * @return
     */
    private int checkAddFreeCount(HotFootballResultLib lib) {
        //SpecialPlay 表里没有配 TYPE_ADD_FREE_COUNT 行时 hotFootballAddFreeInfo 不会被初始化，按"不增加"处理
        if (this.hotFootballAddFreeInfo == null) {
            return 0;
        }
        if (this.hotFootballAddFreeInfo.getLibType() != HotFootballConstant.SpecialMode.FREE) {
            return 0;
        }
        if (lib == null || lib.getIconArr() == null) {
            return 0;
        }

        int addCount = 0;
        for (int i = 1; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            //是否出现了目标图标
            if (icon != this.hotFootballAddFreeInfo.getTargetIcon()) {
                continue;
            }
            boolean flag = PropUtil.calProp(this.hotFootballAddFreeInfo.getProp());
            if (flag) {
                addCount += this.hotFootballAddFreeInfo.getAddFreeCount();
            }
        }
        return addCount;
    }

    /**
     * 修补图标
     */
    public void repairIcons(int libType, int[] arr, List<HotFootballAwardLineInfo> list, List<HotFootballAddIconInfo> addIconInfoList, int winCount) {
        if (list == null || list.isEmpty()) {
            return;
        }

        winCount++;

        //连续中奖后重置中奖倍数（仅普通模式；免费模式由下面能量值机制处理）
        if (libType != HotFootballConstant.SpecialMode.FREE) {
            resetLineRewardTimes(libType, winCount, list);
        }

        //免费模式：先按当前 multiplier 倍数应用到这一波 cascade 的中奖
        //（文档 [37-43] 能量值/守门员：multiplier sticky，每个赢奖符号 +1 能量，满了 +2 multiplier）
        int energyMultiplierForThisCascade = 0;
        int energyDeltaForThisCascade = 0;
        if (libType == HotFootballConstant.SpecialMode.FREE) {
            int[] state = energyMeterTL.get();
            if (state == null) {
                state = new int[]{ENERGY_INITIAL_MULTIPLIER, ENERGY_INITIAL_MAX, 0};
                energyMeterTL.set(state);
            }
            energyMultiplierForThisCascade = state[0];
            //应用 multiplier 到这一波 cascade 所有中奖线的 baseTimes
            final int m = energyMultiplierForThisCascade;
            list.forEach(info -> info.setBaseTimes(info.getBaseTimes() * m));

            //数本 cascade 的"赢奖符号"数量（排除占位符、银/金框）
            for (HotFootballAwardLineInfo info : list) {
                if (info.getSameIconSet() == null) {
                    continue;
                }
                for (Integer idx : info.getSameIconSet()) {
                    int icon = arr[idx];
                    //跳过占位符（大格子的 anchor 已经算一次了，占位符不重复算）
                    if (icon == SlotsConst.Common.PLACEHOLDER_ELEMENTS) {
                        continue;
                    }
                    //跳过银框/金框（有 postChange 配置的就是带框符号，文档明确排除）
                    if (hasPostChangeIcon(icon)) {
                        continue;
                    }
                    energyDeltaForThisCascade++;
                }
            }
        }

        HotFootballAddIconInfo addIconInfo = new HotFootballAddIconInfo();

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);

        //将所有需要消除的图标进行汇总
        Map<Integer, Set<Integer>> allSameMap = new HashMap<>();
        for (HotFootballAwardLineInfo info : list) {
            if (info.getSameIconSet() == null || info.getSameIconSet().isEmpty()) {
                continue;
            }

            //替换成wild的坐标
            Set<Integer> replaceWildIndexs = new HashSet<>();
            Set<Integer> expandedSameIconSet = new HashSet<>(info.getSameIconSet());

            info.getSameIconSet().forEach(index -> {
                int columnId = index / baseInitCfg.getRows();
                if ((index % baseInitCfg.getRows()) != 0) {
                    columnId++;
                }
                allSameMap.computeIfAbsent(columnId, k -> new HashSet<>()).add(index);
                int beginIndex = (columnId - 1) * baseInitCfg.getRows() + 1;
                int endIndex = beginIndex + baseInitCfg.getRows() - 1;
                Set<Integer> expandedIndexes = expandRemovedIndexes(Collections.singleton(index), arr, beginIndex, endIndex);
                expandedSameIconSet.addAll(getRewardIconIndexes(index, arr, beginIndex, endIndex));
                expandedIndexes.forEach(expandedIndex ->
                        allSameMap.computeIfAbsent(getColumnId(expandedIndex, baseInitCfg.getRows()), k -> new HashSet<>()).add(expandedIndex));

                int icon = arr[index];

                //判断消除的图标是不是金色图标
                if (postChangeToWild(icon)) {
                    replaceWildIndexs.add(index);
                }
            });

            info.setSameIconSet(expandedSameIconSet);
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
        List<HotFootballAwardLineInfo> newAwardInfoList = fullLine(arr);

        addIconInfo.setAwardLineInfoList(newAwardInfoList);
        addIconInfoList.add(addIconInfo);

        //免费模式：本 cascade 处理结束后推进能量值；达到满值则 multiplier+2 并提升 maxEnergy
        //这样下一个 cascade（递归调用）就会使用更新后的 multiplier
        if (libType == HotFootballConstant.SpecialMode.FREE && energyDeltaForThisCascade > 0) {
            int[] state = energyMeterTL.get();
            if (state != null) {
                state[2] += energyDeltaForThisCascade;
                while (state[2] >= state[1]) {
                    state[2] -= state[1];
                    state[0] += MULTIPLIER_STEP;
                    if (state[1] < ENERGY_MAX_CAP) {
                        state[1] = Math.min(state[1] + ENERGY_STEP, ENERGY_MAX_CAP);
                    }
                }
            }
        }

        repairIcons(libType, arr, newAwardInfoList, addIconInfoList, winCount);
    }

    private void resetLineRewardTimes(int libType, int winCount, List<HotFootballAwardLineInfo> list) {
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

//        System.out.println("需要消除的坐标 removedIndexes = " + removedIndexes);
//        System.out.println("消除前打印 ");
//        printResult(arr);

        //这一列开始坐标
        int beginIndex = (colIndex - 1) * rows + 1;
        //这一列结束坐标
        int endIndex = beginIndex + rows - 1;
        Set<Integer> expandedRemovedIndexes = expandRemovedIndexes(removedIndexes, arr, beginIndex, endIndex);

        //找到这一列，消除后应该剩余的图标
        //postChangeFlags 与 validIndexes 并列：true 表示该项是 postChange 转换出来的新图标（含其上方占位符）
        List<Integer> validIndexes = new ArrayList<>(baseInitCfg.getRows());
        List<Boolean> postChangeFlags = new ArrayList<>(baseInitCfg.getRows());
        boolean columnHasPostChange = false;
        for (int i = beginIndex; i <= endIndex; i++) {
            int icon = arr[i];
            if (expandedRemovedIndexes.contains(i)) {
                //判断消除的图标是不是金色图标
                Integer replaceIcon = getPostChangeIcon(icon);
                if (replaceIcon != null) {
                    int before = validIndexes.size();
                    addIconWithPlaceholders(validIndexes, replaceIcon);
                    int after = validIndexes.size();
                    for (int v = before; v < after; v++) {
                        postChangeFlags.add(true);
                    }
                    columnHasPostChange = true;
                }
            } else {
                validIndexes.add(icon);
                postChangeFlags.add(false);
            }
            arr[i] = -1;
        }

        validIndexes = validIndexes.reversed();
        postChangeFlags = postChangeFlags.reversed();

        //将剩余的图标重新填充回去
        //若本列出现 postChange，则把整列每一格的最终值都写入 addIconMap：
        //因为有 postChange 时，原图标实际"留在原位"（postChange 产物填了被消除的位置），
        //客户端按常规 drop 推断会算错位，需要服务端把整列状态全发给它做 override
        int curIndex = endIndex;
        for (int i = 0; i < validIndexes.size(); i++) {
            int icon = validIndexes.get(i);
            arr[curIndex] = icon;
            if (columnHasPostChange || postChangeFlags.get(i)) {
                addIconMap.put(curIndex, icon);
            }
            curIndex--;
        }

//        System.out.println("消除后打印 ");
//        printResult(arr);

        Map<Integer, BaseRollerCfg> rollerCfgMap = this.baseRollerCfgMap.entrySet().stream().findFirst().get().getValue();
        BaseRollerCfg baseRollerCfg = rollerCfgMap.get(colIndex);

        int first = baseRollerCfg.getAxleCountScope().get(0) - 1;
        int last = baseRollerCfg.getAxleCountScope().get(1) - 1;
        int scopeSize = last - first + 1;
        List<Integer> rollerElements = baseRollerCfg.getElements();
        int scopeIndex = RandomUtils.randomMinMax(first, last);

        //起点对齐：若随机起点落到大格子 anchor 中段（roller 序列里 anchor 前面有它的 placeholder），
        //回退 (space-1) 步到对应的 placeholder，避免拉出 anchor 时缺少占位符导致客户端表现为空格
        {
            BaseElementCfg startCfg = baseElementCfgMap.get(rollerElements.get(scopeIndex));
            if (startCfg != null && startCfg.getSpace() > 1) {
                scopeIndex -= (startCfg.getSpace() - 1);
                if (scopeIndex < first) {
                    scopeIndex += scopeSize;
                }
            }
        }

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
            if (oldIcon == SlotsConst.Common.PLACEHOLDER_ELEMENTS
                    && getMultiGridAnchorIndex(index, arr, beginIndex, endIndex) > 0) {
                continue;
            }

            int elementId = rollerElements.get(scopeIndex);
            arr[index] = elementId;
            addIconMap.put(index, elementId);
            log.debug("补充新图标 index = {}, icon = {}", index, elementId);

            scopeIndex++;
        }

        //末尾兜底：扫描列内是否存在 dangling placeholder（占位符 0 但下方无对应 anchor），
        //若有则从 roller 继续拉一个非占位符且非大格子的 1 格普通图标替换
        for (int i = 0; i < baseInitCfg.getRows(); i++) {
            int index = beginIndex + i;
            if (arr[index] != SlotsConst.Common.PLACEHOLDER_ELEMENTS) {
                continue;
            }
            if (getMultiGridAnchorIndex(index, arr, beginIndex, endIndex) > 0) {
                continue;
            }
            int safety = scopeSize;
            while (safety-- > 0) {
                if (scopeIndex > last) {
                    scopeIndex = first;
                }
                int el = rollerElements.get(scopeIndex);
                BaseElementCfg cfg = baseElementCfgMap.get(el);
                int sp = (cfg == null) ? 0 : cfg.getSpace();
                scopeIndex++;
                if (el != SlotsConst.Common.PLACEHOLDER_ELEMENTS && sp == 1) {
                    arr[index] = el;
                    addIconMap.put(index, el);
                    log.debug("修补游离占位符 index = {}, icon = {}", index, el);
                    break;
                }
            }
        }

//        System.out.println("补充后打印 ");
//        printResult(arr);
//        System.out.println();
    }

    private Set<Integer> expandRemovedIndexes(Set<Integer> removedIndexes, int[] arr, int beginIndex, int endIndex) {
        Set<Integer> expandedRemovedIndexes = new HashSet<>(removedIndexes);
        for (Integer index : removedIndexes) {
            int anchorIndex = getMultiGridAnchorIndex(index, arr, beginIndex, endIndex);
            if (anchorIndex <= 0) {
                continue;
            }

            BaseElementCfg anchorCfg = baseElementCfgMap.get(arr[anchorIndex]);
            if (anchorCfg == null || anchorCfg.getSpace() <= 1) {
                continue;
            }

            int startIndex = Math.max(beginIndex, anchorIndex - anchorCfg.getSpace() + 1);
            for (int i = startIndex; i <= anchorIndex; i++) {
                expandedRemovedIndexes.add(i);
            }
        }
        return expandedRemovedIndexes;
    }

    private Set<Integer> getRewardIconIndexes(int index, int[] arr, int beginIndex, int endIndex) {
        //大格子（含 postChange 转换的情况）必须把 anchor + 所有 placeholder 一起返回给客户端
        //否则客户端拿到的 rewardIconInfo.iconIndexs 缺占位符索引，会出现「anchor 消除了但 placeholder 还显示为空格」的视觉残留
        return expandRemovedIndexes(Collections.singleton(index), arr, beginIndex, endIndex);
    }

    private boolean hasPostChangeIcon(int icon) {
        return this.baseElementPostChangeMap != null && this.baseElementPostChangeMap.containsKey(icon);
    }

    private boolean postChangeToWild(int icon) {
        if (this.baseElementPostChangeMap == null || this.iconsMap == null) {
            return false;
        }
        PropInfo propInfo = this.baseElementPostChangeMap.get(icon);
        Set<Integer> wildIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_WILD);
        if (propInfo == null || propInfo.getPropMap().isEmpty() || wildIconSet == null || wildIconSet.isEmpty()) {
            return false;
        }
        return propInfo.getPropMap().keySet().stream().allMatch(wildIconSet::contains);
    }

    private int getMultiGridAnchorIndex(int index, int[] arr, int beginIndex, int endIndex) {
        int icon = arr[index];
        BaseElementCfg cfg = baseElementCfgMap.get(icon);
        if (cfg != null && cfg.getSpace() > 1) {
            return index;
        }

        if (icon != SlotsConst.Common.PLACEHOLDER_ELEMENTS) {
            return -1;
        }

        for (int anchorIndex = index + 1; anchorIndex <= endIndex; anchorIndex++) {
            BaseElementCfg anchorCfg = baseElementCfgMap.get(arr[anchorIndex]);
            if (anchorCfg == null || anchorCfg.getSpace() <= 1) {
                continue;
            }
            if (index >= anchorIndex - anchorCfg.getSpace() + 1) {
                return anchorIndex;
            }
        }
        return -1;
    }

    private void addIconWithPlaceholders(List<Integer> validIndexes, int icon) {
        BaseElementCfg cfg = baseElementCfgMap.get(icon);
        int space = cfg == null ? 0 : cfg.getSpace();
        for (int i = 1; i < space; i++) {
            validIndexes.add(SlotsConst.Common.PLACEHOLDER_ELEMENTS);
        }
        validIndexes.add(icon);
    }

    private int getColumnId(int index, int rows) {
        int columnId = index / rows;
        if ((index % rows) != 0) {
            columnId++;
        }
        return columnId;
    }


    @Override
    public void calTimes(HotFootballResultLib lib) throws Exception {
        //先结算常规游戏奖励（连线 + 消除后新增图标），文档要求：触发免费时，先结算常规奖励再触发免费
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        lib.addTimes(calAfterAddIcons(lib.getAddIconInfos()));

        if (triggerFreeLib(lib, HotFootballConstant.SpecialMode.FREE)) {
            //再叠加免费游戏奖励
            lib.addTimes(calFree(lib));
        }
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<HotFootballAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (HotFootballAwardLineInfo awardLineInfo : list) {
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
    public long calAfterAddIcons(List<HotFootballAddIconInfo> addIconInfos) {
        if (addIconInfos == null || addIconInfos.isEmpty()) {
            return 0;
        }

        long times = 0;
        for (HotFootballAddIconInfo info : addIconInfos) {
            if (info.getAwardLineInfoList() == null || info.getAwardLineInfoList().isEmpty()) {
                continue;
            }

            for (HotFootballAwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
                times += awardLineInfo.getBaseTimes();
            }
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        //初始化元素
        Map<Integer, BaseElementCfg> tmpBaseElementCfgMap = new HashMap<>();
        for (BaseElementCfg baseElementCfg : GameDataManager.getBaseElementCfgList()) {
            //游戏id
            if (baseElementCfg.getGameId() == this.gameType) {
                tmpBaseElementCfgMap.put(baseElementCfg.getElementId(), baseElementCfg);
            }
        }
        this.baseElementCfgMap = tmpBaseElementCfgMap;

        Map<Integer, Map<Integer, Integer>> tmpAddTimesMap = new HashMap<>();

        int tmpMaxWinCount = 0;
        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            //连续中奖
            if (cfg.getPlayType() == HotFootballConstant.SpecialPlay.TYPE_CONSECUTIVE_WINS) {
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
            } else if (cfg.getPlayType() == HotFootballConstant.SpecialPlay.TYPE_ADD_FREE_COUNT) {  //增加免费次数
                HotFootballAddFreeInfo tmpHotFootballAddFreeInfo = new HotFootballAddFreeInfo();
                String[] arr = cfg.getValue().split("_");

                tmpHotFootballAddFreeInfo.setLibType(Integer.parseInt(arr[0]));
                tmpHotFootballAddFreeInfo.setTargetIcon(Integer.parseInt(arr[1]));
                tmpHotFootballAddFreeInfo.setAddFreeCount(Integer.parseInt(arr[2]));
                tmpHotFootballAddFreeInfo.setProp(Integer.parseInt(arr[3]));

                this.hotFootballAddFreeInfo = tmpHotFootballAddFreeInfo;
            }
        }
        this.addTimesMap = tmpAddTimesMap;
        this.maxWinCount = tmpMaxWinCount;
    }

    @Override
    public SpecialGirdInfo gridUpdate(int cfgId, int[] arr) {
        log.debug("开始修改格子 specialGirdCfgId = {}", cfgId);
        SpecialGirdCfg specialGirdCfg = GameDataManager.getSpecialGirdCfg(cfgId);
        if (specialGirdCfg == null) {
            log.debug("修改格子未找到对应的配置 cfgId = {}", cfgId);
            return null;
        }

        GirdUpdatePropConfig girdUpdatePropConfig = this.specialGirdCfgMap.get(cfgId);
        if (girdUpdatePropConfig == null) {
            log.debug("修改格子未找到计算后的权重信息 cfgId = {}", cfgId);
            return null;
        }

        if (girdUpdatePropConfig.getRandCountPropInfo() == null) {
            log.debug("修改格子未找到计算后的随机次数权重信息 cfgId = {}", cfgId);
            return null;
        }

        //获取随机次数
        Integer randCount = girdUpdatePropConfig.getRandCountPropInfo().getRandKey();
        if (randCount == null || randCount < 1) {
            return null;
        }

        log.debug("获取到随机次数 cfgId = {},randCount = {}", cfgId, randCount);
        //因为有最大次数限制，所以先clone
        PropInfo cloneAffectGirdPropInfo = girdUpdatePropConfig.getAffectGirdPropInfo().clone();
        //出现的次数记录
        Map<Integer, Integer> girdShowMap = new HashMap<>();

        SpecialGirdInfo info = new SpecialGirdInfo();
        info.setCfgId(specialGirdCfg.getId());

        //记录实际修改格子的次数
        int x = 0;
        int maxForCount = arr.length * 2;
        for (int i = 0; i < maxForCount; i++) {
            //获取一个需要替换的格子
            Integer girdId = cloneAffectGirdPropInfo.getRandKey();
            girdShowMap.merge(girdId, 1, Integer::sum);

            //该格子上的图标id
            int icon = arr[girdId];
            //检查该格子是否不可替换
            if (specialGirdCfg.getNotReplaceEle() != null && specialGirdCfg.getNotReplaceEle().contains(icon)) {
                continue;
            }

            //随机一个需要出现的图标
            int newIcon = girdUpdatePropConfig.getShowIconPropInfo().getRandKey();
            log.debug("修改格子 girdId = {}, oldIcon = {}, newIcon = {}", girdId, arr[girdId], newIcon);

            if (cfgId == HotFootballConstant.SpecialGird.GRID_TWO
                    || cfgId == HotFootballConstant.SpecialGird.GRID_THERE
                    || cfgId == HotFootballConstant.SpecialGird.GRID_FOUR) {
                BaseElementCfg baseElement = baseElementCfgMap.get(newIcon);
                if (baseElement != null && baseElement.getSpace() > 1) {
                    //计算 girdId 所在列的边界，防止大格子越过列顶
                    BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
                    int columnId = getColumnId(girdId, baseInitCfg.getRows());
                    int colBegin = (columnId - 1) * baseInitCfg.getRows() + 1;
                    int colEnd = colBegin + baseInitCfg.getRows() - 1;

                    int oldIcon = arr[girdId];
                    Map<Integer, Integer> updateMap = new HashMap<>();
                    boolean isUpdate = true;

                    //先检查 girdId 自身：若它已经是别的大格子的占位符或 anchor（且不是我们要覆盖的同一位置），
                    //不能强行放新的大格子（会把原大格子结构打破，导致 anchor 没占位符 / 占位符没 anchor）
                    {
                        int existingAnchor = getMultiGridAnchorIndex(girdId, arr, colBegin, colEnd);
                        if (existingAnchor > 0 && existingAnchor != girdId) {
                            isUpdate = false;
                        }
                    }

                    if (isUpdate) {
                        for (int i1 = 1; i1 < baseElement.getSpace(); i1++) {
                            int upperIdx = girdId - i1;
                            //向上越界（出列顶），无法容纳大格子
                            if (upperIdx < colBegin) {
                                isUpdate = false;
                                break;
                            }
                            //上方的格子若属于别的大格子（无论是 anchor 还是占位符），都不能覆盖：
                            //  - 是 anchor 时覆盖会丢失大格子主体
                            //  - 是占位符时覆盖会让对应 anchor 失去占位，结构断裂
                            int upperAnchor = getMultiGridAnchorIndex(upperIdx, arr, colBegin, colEnd);
                            if (upperAnchor > 0 && upperAnchor != girdId) {
                                isUpdate = false;
                                break;
                            }
                            //其它情况（普通 1 格图标、孤立占位符等）替换为占位符
                            updateMap.put(upperIdx, SlotsConst.Common.PLACEHOLDER_ELEMENTS);
                        }
                    }

                    updateMap.put(girdId, newIcon);
                    if (isUpdate) {
                        updateMap.forEach((k, v) -> arr[k] = v);
                        log.debug("修改大格子 cfgId = {}, oldIcon = {}, newIcon = {}, newArr = {}", cfgId, oldIcon, newIcon, JSONObject.toJSONString(arr));
                    }
                }
            } else {
                arr[girdId] = newIcon;
            }
            //赋值
            if (girdUpdatePropConfig.getValuePropInfo() != null) {
                int value = girdUpdatePropConfig.getValuePropInfo().getRandKey();
                info.addValue(girdId, value);
                log.debug("赋值 girdId = {}, value = {}", girdId, value);
            }

            //达到最大次数限制后，移除
            if (girdShowMap.get(girdId) >= cloneAffectGirdPropInfo.getMaxShowLimit(girdId)) {
                cloneAffectGirdPropInfo.removeKeyAndRecalculate(girdId);
            }

            x++;
            if (x >= randCount) {
                break;
            }
        }

        //值类型
        if (specialGirdCfg.getValueType() != null && !specialGirdCfg.getValueType().isEmpty()) {
            info.setValueType(specialGirdCfg.getValueType().get(0));
            info.setMiniGameId(specialGirdCfg.getValueType().get(1));
        }

        log.debug("修改后的图标 arr = {}", Arrays.toString(arr));
        return info;
    }

    public Map<Integer, Map<Integer, Integer>> getAddTimesMap() {
        return addTimesMap;
    }

    protected void printResult(int[] arr) {
        BaseInitCfg cfg = GameDataManager.getBaseInitCfg(this.gameType);

        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= cfg.getRows(); i++) {
            for (int j = 0; j < cfg.getCols(); j++) {
                int index = cfg.getRows() * j + i;
                int id = arr[index];
                sb.append(id);
                if (id < 10) {
                    sb.append("   ");
                } else {
                    sb.append("  ");
                }
            }
            sb.append("\n");
        }
        System.out.println(sb);
    }
}
