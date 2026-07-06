package com.jjg.game.slots.game.superGolf.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.utils.PropUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementCfg;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRollerCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.game.superGolf.SuperGolfConstant;
import com.jjg.game.slots.game.superGolf.data.SuperGolfAddFreeInfo;
import com.jjg.game.slots.game.superGolf.data.SuperGolfAddIconInfo;
import com.jjg.game.slots.game.superGolf.data.SuperGolfAwardLineInfo;
import com.jjg.game.slots.game.superGolf.data.SuperGolfResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 超级高尔夫结果生成器。
 * <p>
 * 跟 Dracula 的核心区别在 cascade 处理：
 * <ol>
 *   <li>初始 fullLine 匹配中奖</li>
 *   <li>cascade：中奖符号里如果是"带框符号"（{@link #isBoxedIcon}），<b>在新一轮回合里转换为神秘符号 id=114</b>
 *       而不是普通消除（文档 [28]）</li>
 *   <li>cascade 跑完无中奖时：</li>
 *   <ul>
 *     <li>统计盘面上神秘符号个数 N</li>
 *     <li>若 N&gt;=1：把所有 mystery 同时转换为 <b>一个</b>随机普通符号（排除百搭/夺宝/神秘）</li>
 *     <li>重新 fullLine：如有中奖，baseTimes × N（这就是奖金倍数）；无中奖则倍率消失，下轮再来</li>
 *   </ul>
 *   <li>免费模式下倍率累计到 PlayerGameData.freeMultiplierAccum（由 AbstractSuperGolfGameManager.free 处理）</li>
 * </ol>
 */
@Component
public class SuperGolfGenerateManager extends AbstractSlotsGenerateManager<SuperGolfAwardLineInfo, SuperGolfResultLib> {

    private SuperGolfAddFreeInfo superGolfAddFreeInfo;
    private Map<Integer, BaseElementCfg> baseElementCfgMap;

    public SuperGolfGenerateManager() {
        super(SuperGolfResultLib.class);
    }

    // ============================ checkAward 主流程 ============================

    @Override
    public SuperGolfResultLib checkAward(int[] arr, SuperGolfResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);

        List<SuperGolfAddIconInfo> addIconInfoList = new ArrayList<>();
        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);

        //基类 generateAllIcons 顺序读 roller，随机起点落到 anchor 时上方可能不是占位符。
        //在做任何中奖判定前，先把每列的多格符号占位符对齐：anchor 上方 (space-1) 格强制写成 PLACEHOLDER_ELEMENTS=0
        alignMultiGridPlaceholders(newArr);

        //开局即时检查：若初始盘面（含 SpecialGird 摆放）已凑齐 3+ mystery，先把它们转换掉，
        //转换信息记录到 addIconInfoList[0] 作为"初始 mystery 触发事件"（无 addIconMap 转移、仅 mystery 替换）。
        //转换后的中奖归入 lib.awardLineInfoList，避免和 cascade 链重复。
        MysteryTriggerResult initialTrigger = doMysteryTriggerIfNeeded(newArr);

        //1. 初始中奖检查（基于已经转换过的盘面）
        lib.setIconArr(newArr);
        List<SuperGolfAwardLineInfo> fullLineInfoList = fullLine(lib);
        if (initialTrigger != null && fullLineInfoList != null && !fullLineInfoList.isEmpty()) {
            int m = initialTrigger.multiplier;
            for (SuperGolfAwardLineInfo info : fullLineInfoList) {
                info.setBaseTimes(info.getBaseTimes() * m);
            }
            lib.setMultiplier(m);
        }
        lib.addAllAwardLineInfo(fullLineInfoList);

        if (initialTrigger != null) {
            //初始触发：作为 cascade 链的第一条事件记录（仅 mystery 转换信息，无 cascade 消除）
            SuperGolfAddIconInfo initEvent = new SuperGolfAddIconInfo();
            initEvent.setAddIconMap(initialTrigger.replaceMap);
            initEvent.setMysteryConvertedToIcon(initialTrigger.targetIcon);
            initEvent.setMultiplier(initialTrigger.multiplier);
            //awardLineInfoList 留空：本次触发的中奖在 lib.awardLineInfoList，避免重复计数
            addIconInfoList.add(initEvent);
        }

        //2. 全局分散：4+ scatter 触发免费
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        //3. cascade 链：消除/补图/带框转神秘；每一轮 cascade 后立即检查 mystery
        repairIcons(newArr, lib.getAwardLineInfoList(), addIconInfoList, lib);

        if (!addIconInfoList.isEmpty()) {
            lib.setAddIconInfos(addIconInfoList);
        }

        calTimes(lib);
        return lib;
    }

    @Override
    protected SuperGolfAwardLineInfo addFullLineAwardInfo(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg, int[] arr) {
        SuperGolfAwardLineInfo info = super.addFullLineAwardInfo(sameIconIndexSet, cfg, arr);
        info.setSameIcon(cfg.getElementId().getFirst() % 1000);
        return info;
    }

    @Override
    protected SuperGolfAwardLineInfo getAwardLineInfo() {
        return new SuperGolfAwardLineInfo();
    }

    // ============================ cascade 链 ============================

    /**
     * cascade 链：每轮把中奖符号"消除/带框转神秘"，然后下落、补图。
     * <p>
     * 文档 [28]：带框符号若参与中奖，<b>不消除</b>，而是在下一回合转换为神秘符号。
     * <p>
     * 本轮 cascade 处理完后立即检查 mystery 数：≥3 → 触发 mystery 转换；否则继续下一轮 fullLine。
     */
    public void repairIcons(int[] arr, List<SuperGolfAwardLineInfo> list, List<SuperGolfAddIconInfo> addIconInfoList, SuperGolfResultLib lib) {
        if (list == null || list.isEmpty()) {
            return;
        }

        SuperGolfAddIconInfo addIconInfo = new SuperGolfAddIconInfo();
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);

        //把所有"中奖坐标"按列汇总
        Map<Integer, Set<Integer>> colToWinIndexes = new HashMap<>();
        //本轮要转换为神秘符号的坐标（带框符号且参与中奖的位置）
        Set<Integer> toMystery = new HashSet<>();

        for (SuperGolfAwardLineInfo info : list) {
            if (info.getSameIconSet() == null || info.getSameIconSet().isEmpty()) {
                continue;
            }
            Set<Integer> sameSet = info.getSameIconSet();
            Set<Integer> boxedToMystery = new HashSet<>();

            for (Integer idx : sameSet) {
                int columnId = getColumnId(idx, baseInitCfg.getRows());
                colToWinIndexes.computeIfAbsent(columnId, k -> new HashSet<>()).add(idx);
                if (isBoxedIcon(arr[idx])) {
                    boxedToMystery.add(idx);
                }
            }
            if (!boxedToMystery.isEmpty()) {
                info.setBoxedToMysteryIndexes(boxedToMystery);
                toMystery.addAll(boxedToMystery);
            }
        }

        if (!toMystery.isEmpty()) {
            addIconInfo.setTurnToMysteryIndexes(new ArrayList<>(toMystery));
        }

        //逐列处理消除/下落/补图：带框中奖位"原地变神秘"，其余中奖位被消除并下落补充
        Map<Integer, Integer> addIconMap = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> en : colToWinIndexes.entrySet()) {
            int colIndex = en.getKey();
            Set<Integer> winIndexes = en.getValue();
            processIcons(colIndex, winIndexes, toMystery, arr, addIconMap);
        }
        addIconInfo.setAddIconMap(addIconMap);

        //本轮 cascade 完成 → 任意时刻盘面 mystery 数凑齐 3 就立刻触发：
        //  转换所有 mystery → 一个随机 normal 符号，把转换信息合并到本 cascade 的 addIconInfo 上
        MysteryTriggerResult trigger = doMysteryTriggerIfNeeded(arr);
        if (trigger != null) {
            addIconInfo.setMysteryConvertedToIcon(trigger.targetIcon);
            addIconInfo.setMultiplier(trigger.multiplier);
            //把"mystery 转换为 targetIcon"的位置也并到 addIconMap，给客户端动画用
            if (addIconInfo.getAddIconMap() != null && trigger.replaceMap != null) {
                addIconInfo.getAddIconMap().putAll(trigger.replaceMap);
            }
            lib.setMultiplier(trigger.multiplier);
        }

        //再做新一轮 fullLine（基于转换后的盘面）
        List<SuperGolfAwardLineInfo> newAwardInfoList = fullLine(arr);
        if (trigger != null && newAwardInfoList != null && !newAwardInfoList.isEmpty()) {
            //mystery 触发的中奖：baseTimes × N
            int m = trigger.multiplier;
            for (SuperGolfAwardLineInfo info : newAwardInfoList) {
                info.setBaseTimes(info.getBaseTimes() * m);
            }
        }
        addIconInfo.setAwardLineInfoList(newAwardInfoList);
        addIconInfoList.add(addIconInfo);

        repairIcons(arr, newAwardInfoList, addIconInfoList, lib);
    }

    /**
     * mystery 转换执行结果。
     */
    private static class MysteryTriggerResult {
        int targetIcon;
        int multiplier;
        Map<Integer, Integer> replaceMap;
    }

    /**
     * 盘面 mystery 数 ≥ {@link SuperGolfConstant.Common#MYSTERY_TRIGGER_MIN} 时，
     * 同时把所有 mystery 替换为一个随机 normal 符号；否则返回 null。
     * <p>
     * 注意：此方法只负责"替换 arr 内容"，<b>不</b>把中奖结算写到 lib/addIconInfoList。
     * 调用方拿到 result 后自己决定怎么记录（初始触发 / cascade 触发 写到不同位置）。
     */
    private MysteryTriggerResult doMysteryTriggerIfNeeded(int[] arr) {
        int mysteryCount = countMystery(arr);
        if (mysteryCount < SuperGolfConstant.Common.MYSTERY_TRIGGER_MIN) {
            return null;
        }
        int targetIcon = pickRandomNormalIcon();
        if (targetIcon <= 0) {
            return null;
        }
        Map<Integer, Integer> replaceMap = new HashMap<>();
        for (int i = 1; i < arr.length; i++) {
            int icon = arr[i];
            if (icon == SuperGolfConstant.BaseElement.MYSTERY
                    || icon == SuperGolfConstant.BaseElement.MYSTERY_TWO
                    || icon == SuperGolfConstant.BaseElement.MYSTERY_THREE) {
                arr[i] = targetIcon;
                replaceMap.put(i, targetIcon);
            }
        }
        MysteryTriggerResult result = new MysteryTriggerResult();
        result.targetIcon = targetIcon;
        result.multiplier = mysteryCount;
        result.replaceMap = replaceMap;
        return result;
    }

    /**
     * 处理一列的消除、下落、补图（含大符号占位处理）。
     * <p>
     * 关键点：
     * <ul>
     *   <li><b>大符号消除</b>：anchor 中奖时，{@link #expandRemovedIndexes} 把其上方占位符也一并加入消除集，
     *       避免 anchor 没了占位符还留着</li>
     *   <li><b>带框→神秘</b>：带框 anchor 中奖时，仅 anchor 位变 mystery（一个符号），占位符位置空出走补图</li>
     *   <li><b>大符号补图</b>：从 roller 拉到 anchor 时用 {@link #addIconWithPlaceholders} 在其前方塞占位符；
     *       随机起点落在 anchor 中段时回退 (space-1) 步对齐到占位符</li>
     * </ul>
     */
    private void processIcons(int colIndex, Set<Integer> winIndexes, Set<Integer> boxedToMysteryIndexes, int[] arr, Map<Integer, Integer> addIconMap) {
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        int rows = baseInitCfg.getRows();
        int beginIndex = (colIndex - 1) * rows + 1;
        int endIndex = beginIndex + rows - 1;

        //把中奖位扩展到大符号占位符（anchor 中奖时占位符也算被消除）
        Set<Integer> expandedRemovedIndexes = expandRemovedIndexes(winIndexes, arr, beginIndex, endIndex);

        //带框→神秘：仅作用在 anchor 自身（占位符不变 mystery，让后续走补图）
        for (int idx : boxedToMysteryIndexes) {
            if (idx < beginIndex || idx > endIndex) continue;
            int mysteryId = getMysteryIdBySpace(arr[idx]);
            arr[idx] = mysteryId;
            addIconMap.put(idx, mysteryId);
            //占位符随后被清零等补图（从 expandedRemovedIndexes 里保留，避免被算作"存活"）
        }

        //收集本列存活的图标，被消除位（除了刚变 mystery 的 anchor）清成 -1
        List<Integer> validIcons = new ArrayList<>(rows);
        for (int i = beginIndex; i <= endIndex; i++) {
            int icon = arr[i];
            if (boxedToMysteryIndexes.contains(i)) {
                //已原地变 mystery → 算"存活"，加入 validIcons 保留
                validIcons.add(icon);
                arr[i] = -1;
                continue;
            }
            if (expandedRemovedIndexes.contains(i)) {
                //普通消除位（含 anchor + 占位符）
                arr[i] = -1;
            } else {
                validIcons.add(icon);
                arr[i] = -1;
            }
        }

        //自下而上回填存活图标
        Collections.reverse(validIcons);
        int curIndex = endIndex;
        for (int icon : validIcons) {
            arr[curIndex] = icon;
            curIndex--;
        }

        //从顶部补图（处理大符号 anchor + 占位符）
        BaseRollerCfg rollerCfg = getFirstRollerCfg(colIndex);
        if (rollerCfg == null || rollerCfg.getElements() == null || rollerCfg.getAxleCountScope() == null) {
            return;
        }
        List<Integer> elements = rollerCfg.getElements();
        int first = rollerCfg.getAxleCountScope().get(0) - 1;
        int last = rollerCfg.getAxleCountScope().get(1) - 1;
        int scopeSize = last - first + 1;
        int scopeIndex = RandomUtils.randomMinMax(first, last);

        //起点对齐：若随机起点落到 anchor 中段，回退到对应占位符位置
        {
            BaseElementCfg startCfg = baseElementCfgMap == null ? null : baseElementCfgMap.get(elements.get(scopeIndex));
            if (startCfg != null && startCfg.getSpace() > 1) {
                scopeIndex -= (startCfg.getSpace() - 1);
                if (scopeIndex < first) scopeIndex += scopeSize;
            }
        }

        for (int i = 0; i < rows; i++) {
            int index = beginIndex + i;
            int oldIcon = arr[index];
            if (oldIcon > 0) continue;
            //本格如果是某个大符号的占位符空位，跳过让 anchor 拉时一并塞
            if (oldIcon == SlotsConst.Common.PLACEHOLDER_ELEMENTS
                    && getMultiGridAnchorIndex(index, arr, beginIndex, endIndex) > 0) {
                continue;
            }

            if (scopeIndex > last) scopeIndex = first;
            int elementId = elements.get(scopeIndex);
            BaseElementCfg cfg = baseElementCfgMap == null ? null : baseElementCfgMap.get(elementId);
            int space = (cfg == null || cfg.getSpace() <= 0) ? 1 : cfg.getSpace();

            if (space > 1) {
                //大符号：在 index 上方 (space-1) 格写占位符，本格写 anchor
                int neededTop = index - (space - 1);
                if (neededTop < beginIndex) {
                    //空间不够放完整大符号 → 退化为 1 格
                    arr[index] = elementId;
                    addIconMap.put(index, elementId);
                } else {
                    for (int p = neededTop; p < index; p++) {
                        arr[p] = SlotsConst.Common.PLACEHOLDER_ELEMENTS;
                        addIconMap.put(p, SlotsConst.Common.PLACEHOLDER_ELEMENTS);
                    }
                    arr[index] = elementId;
                    addIconMap.put(index, elementId);
                    //把 i 推到大符号顶部（占位符），让外层 for 跳过这几格
                    i += (space - 1);
                }
            } else {
                arr[index] = elementId;
                addIconMap.put(index, elementId);
            }
            scopeIndex++;
        }

        //末尾兜底：如果列内还残留游离 PLACEHOLDER（占位符 0 但下方无 anchor），
        //从 roller 继续拉 1 格普通符号替换
        for (int i = 0; i < rows; i++) {
            int index = beginIndex + i;
            if (arr[index] != SlotsConst.Common.PLACEHOLDER_ELEMENTS) continue;
            if (getMultiGridAnchorIndex(index, arr, beginIndex, endIndex) > 0) continue;
            int safety = scopeSize;
            while (safety-- > 0) {
                if (scopeIndex > last) scopeIndex = first;
                int el = elements.get(scopeIndex);
                scopeIndex++;
                BaseElementCfg cfg = baseElementCfgMap == null ? null : baseElementCfgMap.get(el);
                int sp = (cfg == null) ? 0 : cfg.getSpace();
                if (el != SlotsConst.Common.PLACEHOLDER_ELEMENTS && sp == 1) {
                    arr[index] = el;
                    addIconMap.put(index, el);
                    break;
                }
            }
        }
    }

    // ============================ 大符号 / 占位符辅助方法 ============================

    /**
     * 初始盘面对齐：扫描每列，发现 anchor（{@link BaseElementCfg#getSpace()}&gt;1）时，
     * 把它上方 (space-1) 格强制写成 {@link SlotsConst.Common#PLACEHOLDER_ELEMENTS}=0。
     * <p>
     * 修复"基类 generateAllIcons 顺序读 roller，随机起点落到 anchor 时上方变成普通图标"的问题。
     * <p>
     * 处理顺序：top-to-bottom（从列顶到列底）。遇到 anchor 时直接覆写上方为占位符，
     * 不考虑罕见的"两个 anchor 重叠"情况——这是配表错误，不在 runtime 兜底范围内。
     */
    private void alignMultiGridPlaceholders(int[] arr) {
        if (baseElementCfgMap == null) return;
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        if (baseInitCfg == null) return;
        int rows = baseInitCfg.getRows();
        int cols = baseInitCfg.getCols();
        for (int col = 1; col <= cols; col++) {
            int beginIndex = (col - 1) * rows + 1;
            int endIndex = beginIndex + rows - 1;
            // 从下往上扫：下方 anchor 先写占位符，上方遇到已被覆盖的格子直接跳过，避免孤儿占位符
            for (int i = endIndex; i >= beginIndex; i--) {
                int icon = arr[i];
                if (icon <= 0) continue;
                BaseElementCfg cfg = baseElementCfgMap.get(icon);
                int space = (cfg == null || cfg.getSpace() <= 0) ? 1 : cfg.getSpace();
                if (space <= 1) continue;
                int topPos = i - (space - 1);
                if (topPos < beginIndex) {
                    log.warn("alignMultiGridPlaceholders: anchor 太靠列顶放不下 col={}, anchorIndex={}, space={}", col, i, space);
                    arr[i] = SlotsConst.Common.PLACEHOLDER_ELEMENTS;
                    continue;
                }
                for (int p = topPos; p < i; p++) {
                    arr[p] = SlotsConst.Common.PLACEHOLDER_ELEMENTS;
                }
            }
        }
    }

    /**
     * 把消除集扩展到大符号占位符。
     * <p>
     * 如果某个 removed 坐标是大符号 anchor，把它上方 (space-1) 个占位符位置也加入消除集。
     * 如果某个 removed 坐标本身就是占位符，找到它的 anchor，然后把整个 [anchor-space+1, anchor] 区间都加入。
     */
    private Set<Integer> expandRemovedIndexes(Set<Integer> removedIndexes, int[] arr, int beginIndex, int endIndex) {
        Set<Integer> result = new HashSet<>(removedIndexes);
        if (baseElementCfgMap == null) return result;
        for (Integer index : removedIndexes) {
            int anchorIndex = getMultiGridAnchorIndex(index, arr, beginIndex, endIndex);
            if (anchorIndex <= 0) continue;
            BaseElementCfg anchorCfg = baseElementCfgMap.get(arr[anchorIndex]);
            if (anchorCfg == null || anchorCfg.getSpace() <= 1) continue;
            int startIndex = Math.max(beginIndex, anchorIndex - anchorCfg.getSpace() + 1);
            for (int i = startIndex; i <= anchorIndex; i++) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * 找到 index 所属"大符号"的 anchor 位置（anchor 一般是大符号底部）。
     * <p>
     * 规则：
     * <ul>
     *   <li>index 自身是 space&gt;1 的图标 → 它就是 anchor，返回 index</li>
     *   <li>index 是 PLACEHOLDER_ELEMENTS → 向下找最近的 space&gt;1 anchor，
     *       并校验 index 在 [anchor-space+1, anchor] 范围内</li>
     *   <li>其他情况返回 -1</li>
     * </ul>
     */
    private int getMultiGridAnchorIndex(int index, int[] arr, int beginIndex, int endIndex) {
        if (baseElementCfgMap == null) return -1;
        int icon = arr[index];
        BaseElementCfg cfg = baseElementCfgMap.get(icon);
        if (cfg != null && cfg.getSpace() > 1) return index;
        if (icon != SlotsConst.Common.PLACEHOLDER_ELEMENTS) return -1;
        for (int anchorIndex = index + 1; anchorIndex <= endIndex; anchorIndex++) {
            BaseElementCfg anchorCfg = baseElementCfgMap.get(arr[anchorIndex]);
            if (anchorCfg == null || anchorCfg.getSpace() <= 1) continue;
            if (index >= anchorIndex - anchorCfg.getSpace() + 1) return anchorIndex;
        }
        return -1;
    }

    private BaseRollerCfg getFirstRollerCfg(int colIndex) {
        if (this.baseRollerCfgMap == null || this.baseRollerCfgMap.isEmpty()) {
            return null;
        }
        Map<Integer, BaseRollerCfg> rollerCfgMap = this.baseRollerCfgMap.entrySet().stream().findFirst().get().getValue();
        return rollerCfgMap == null ? null : rollerCfgMap.get(colIndex);
    }

    private int getColumnId(int index, int rows) {
        int columnId = index / rows;
        if ((index % rows) != 0) {
            columnId++;
        }
        return columnId;
    }

    // ============================ 神秘符号机制（核心差异） ============================

    private int countMystery(int[] arr) {
        int count = 0;
        for (int i = 1; i < arr.length; i++) {
            int icon = arr[i];
            if (icon == SuperGolfConstant.BaseElement.MYSTERY
                    || icon == SuperGolfConstant.BaseElement.MYSTERY_TWO
                    || icon == SuperGolfConstant.BaseElement.MYSTERY_THREE) {
                count++;
            }
        }
        return count;
    }

    /**
     * 从普通中奖符号里加权随机抽一个（排除百搭/夺宝/神秘/奖池）。
     * 简单实现：所有 101-111 等权重均分。如果策划需要按权重抽，可改用 SpecialPlay 配表。
     */
    private int pickRandomNormalIcon() {
        int range = SuperGolfConstant.BaseElement.NORMAL_MAX - SuperGolfConstant.BaseElement.NORMAL_MIN + 1;
        return SuperGolfConstant.BaseElement.NORMAL_MIN + RandomUtils.nextInt(range);
    }

    private int getMysteryIdBySpace(int icon) {
        BaseElementCfg cfg = baseElementCfgMap == null ? null : baseElementCfgMap.get(icon);
        int space = (cfg == null || cfg.getSpace() <= 0) ? 1 : cfg.getSpace();
        return switch (space) {
            case 2 -> SuperGolfConstant.BaseElement.MYSTERY_TWO;
            case 3 -> SuperGolfConstant.BaseElement.MYSTERY_THREE;
            default -> SuperGolfConstant.BaseElement.MYSTERY;
        };
    }

    /**
     * 判断一个图标是不是"带框符号"（参与中奖后会变神秘）。
     * <p>
     * 由策划在 BaseElement 表用 postChange 配置带框 → 普通 / 带框 → 大符号 等映射来表示。
     * 本游戏简化：凡是 baseElementPostChangeMap 里有 postChange 配置的图标，认为是"带框"。
     */
    private boolean isBoxedIcon(int icon) {
        return this.baseElementPostChangeMap != null && this.baseElementPostChangeMap.containsKey(icon);
    }

    // ============================ free 触发：4+ scatter ============================

    @Override
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg,
                               SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }
        int remainFreeCount = freeCount;
        int[] guard = freeGenTotalGuard.get();
        boolean isRoot = (guard == null);
        if (isRoot) {
            guard = new int[]{0, 0};
            freeGenTotalGuard.set(guard);
        }
        guard[1]++;
        try {
            while (remainFreeCount > 0) {
                if (guard[0] >= SlotsConst.Common.MAX_FREE_GAME_TOTAL || guard[1] > SlotsConst.Common.MAX_FREE_DEEP_TOTAL) {
                    log.error("免费生成达到硬上限 gameType={},miniGameId={},剩余={}", this.gameType, specialAuxiliaryCfg.getId(), remainFreeCount);
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
                SuperGolfResultLib lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
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
            }
        }
    }

    /**
     * 免费模式里盘面再触发免费的次数：4 scatter→10, 5→12, 6→14（文档 [40]）。
     * 跟 Dracula 的 12/14/16 是不同表，从 SuperGolfAddFreeInfo 查。
     */
    private int checkAddFreeCount(SuperGolfResultLib lib) {
        if (this.superGolfAddFreeInfo == null) {
            return 0;
        }
        if (this.superGolfAddFreeInfo.getLibType() != SuperGolfConstant.SpecialMode.FREE) {
            return 0;
        }
        if (lib == null || lib.getIconArr() == null) {
            return 0;
        }
        int scatterCount = 0;
        for (int i = 1; i < lib.getIconArr().length; i++) {
            if (lib.getIconArr()[i] == this.superGolfAddFreeInfo.getTargetIcon()) {
                scatterCount++;
            }
        }
        if (scatterCount <= 0) {
            return 0;
        }
        int addFree = this.superGolfAddFreeInfo.getAddFreeCount(scatterCount);
        if (addFree <= 0) {
            return 0;
        }
        int prop = this.superGolfAddFreeInfo.getProp(scatterCount);
        if (prop > 0 && !PropUtil.calProp(prop)) {
            return 0;
        }
        return addFree;
    }

    // ============================ 倍数计算 ============================

    @Override
    public void calTimes(SuperGolfResultLib lib) throws Exception {
        //先结算常规中奖（初始 + 所有 cascade）
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        lib.addTimes(calAfterAddIcons(lib.getAddIconInfos()));

        if (triggerFreeLib(lib, SuperGolfConstant.SpecialMode.FREE)) {
            lib.addTimes(calFree(lib));
        }
    }

    public int calLineTimes(List<SuperGolfAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int times = 0;
        for (SuperGolfAwardLineInfo info : list) {
            times += info.getBaseTimes();
        }
        return times;
    }

    public long calAfterAddIcons(List<SuperGolfAddIconInfo> addIconInfos) {
        if (addIconInfos == null || addIconInfos.isEmpty()) {
            return 0;
        }
        long times = 0;
        for (SuperGolfAddIconInfo info : addIconInfos) {
            if (info.getAwardLineInfoList() == null || info.getAwardLineInfoList().isEmpty()) {
                continue;
            }
            for (SuperGolfAwardLineInfo lineInfo : info.getAwardLineInfoList()) {
                times += lineInfo.getBaseTimes();
            }
        }
        return times;
    }

    // ============================ 配表加载 ============================

    @Override
    protected void specialPlayConfig() {
        //初始化元素
        Map<Integer, BaseElementCfg> tmpBaseElementCfgMap = new HashMap<>();
        for (BaseElementCfg baseElementCfg : GameDataManager.getBaseElementCfgList()) {
            if (baseElementCfg.getGameId() == this.gameType) {
                tmpBaseElementCfgMap.put(baseElementCfg.getElementId(), baseElementCfg);
            }
        }
        this.baseElementCfgMap = tmpBaseElementCfgMap;

        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }
            if (cfg.getPlayType() == SuperGolfConstant.SpecialPlay.TYPE_ADD_FREE_COUNT) {
                //格式：libType,targetIcon,count_addFreeCount_prop|...
                //例如 2,113,4_10_10000|5_12_10000|6_14_10000
                String[] arr = cfg.getValue().split(",");
                if (arr.length < 3) {
                    log.warn("TYPE_ADD_FREE_COUNT 格式异常，跳过 value={}", cfg.getValue());
                    continue;
                }
                SuperGolfAddFreeInfo tmp = new SuperGolfAddFreeInfo();
                tmp.setLibType(Integer.parseInt(arr[0]));
                tmp.setTargetIcon(Integer.parseInt(arr[1]));
                for (String group : arr[2].split("\\|")) {
                    String[] parts = group.split("_");
                    if (parts.length < 3) {
                        continue;
                    }
                    tmp.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                }
                this.superGolfAddFreeInfo = tmp;
                log.info("TYPE_ADD_FREE_COUNT 加载完成 libType={},targetIcon={},tiers={}",
                        tmp.getLibType(), tmp.getTargetIcon(), tmp.getCountToAddFree());
            }
        }
    }

    public SuperGolfAddFreeInfo getSuperGolfAddFreeInfo() {
        return superGolfAddFreeInfo;
    }
}
