package com.jjg.game.slots.game.garaGemstone3.manager;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.sampledata.bean.BaseRollerCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.garaGemstone3.GaraGemstone3Constant;
import com.jjg.game.slots.game.garaGemstone3.data.GaraGemstone3AwardLineInfo;
import com.jjg.game.slots.game.garaGemstone3.data.GaraGemstone3MultiplyAxisInfo;
import com.jjg.game.slots.game.garaGemstone3.data.GaraGemstone3ResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import jodd.util.StringUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GaraGemstone3GenerateManager extends AbstractSlotsGenerateManager<GaraGemstone3AwardLineInfo, GaraGemstone3ResultLib> {
    public GaraGemstone3GenerateManager() {
        super(GaraGemstone3ResultLib.class);
    }

    /** 多图标替换→原图标 静态映射，由 GaraGemstone3Constant.BaseElement 中各图标组构建 */
    private static final Map<Integer, Integer> MULTI_TO_ORIGINAL_MAP;
    static {
        MULTI_TO_ORIGINAL_MAP = new HashMap<>();
        for (int[] group : new int[][]{
                GaraGemstone3Constant.BaseElement.ID_J,
                GaraGemstone3Constant.BaseElement.ID_Q,
                GaraGemstone3Constant.BaseElement.ID_K,
                GaraGemstone3Constant.BaseElement.ID_A,
                GaraGemstone3Constant.BaseElement.ID_GREEN,
                GaraGemstone3Constant.BaseElement.ID_BLUE,
                GaraGemstone3Constant.BaseElement.ID_RED,
                GaraGemstone3Constant.BaseElement.ID_WILD,
        }) {
            for (int i = 1; i < group.length; i++) {
                MULTI_TO_ORIGINAL_MAP.put(group[i], group[0]);
            }
        }
    }

    /**
     * 倍数轴配置列表，从 SpecialPlay 5055023 加载
     */
    private List<GaraGemstone3MultiplyAxisInfo> multiplyAxisInfoList;
    /**
     * 倍数轴权重总和，用于加权随机
     */
    private int multiplyAxisWeightTotal;

    /** 多格图标替换个数配置（5055022），每条 int[]{count, weight} */
    private List<int[]> expandNumList;
    private int expandNumWeightTotal;
    /** 多格图标元素配置（5055021），每条 int[]{elementId, multiplier, weight} */
    private List<int[]> expandIconList;
    private int expandIconWeightTotal;

    @Override
    protected GaraGemstone3AwardLineInfo getAwardLineInfo() {
        return new GaraGemstone3AwardLineInfo();
    }

    @Override
    protected void specialPlayConfig() {
        loadMultiplyAxisConfig();
        loadExpandConfig();
    }

    /**
     * 从 SpecialPlay.xlsx 5055011 加载倍数轴配置。
     * 配置格式：iconId_times_weight|iconId_times_weight|...
     * iconId : 符号ID（需在 BaseRoller 20550114 elements 中存在）
     * times  : 倍数值（1/2/3/5/10/15），奖金符号填0
     * weight : 权重
     * 示例：10_1_5000|11_2_3000|12_3_1000|13_5_500|14_10_400|15_15_100
     */
    private void loadMultiplyAxisConfig() {
        SpecialPlayCfg cfg = GameDataManager.getSpecialPlayCfg(GaraGemstone3Constant.BaseRollerGroup.MULTIPLY_AXIS_CFG_ID);
        if (cfg == null || StringUtil.isEmpty(cfg.getValue())) {
            log.warn("倍数轴配置为空，gameType={}, cfgId={}", this.gameType, GaraGemstone3Constant.BaseRollerGroup.MULTIPLY_AXIS_CFG_ID);
            return;
        }
        String[] entries = cfg.getValue().split("\\|");
        List<GaraGemstone3MultiplyAxisInfo> list = new ArrayList<>(entries.length);
        int weightTotal = 0;
        for (String entry : entries) {
            String[] parts = entry.split("_");
            if (parts.length < 3) {
                log.warn("倍数轴配置格式错误，跳过：{}", entry);
                continue;
            }
            GaraGemstone3MultiplyAxisInfo info = new GaraGemstone3MultiplyAxisInfo();
            info.setIconId(Integer.parseInt(parts[0]));
            info.setTimes(Integer.parseInt(parts[1]));
            info.setWeight(Integer.parseInt(parts[2]));
            weightTotal += info.getWeight();
            list.add(info);
        }
        this.multiplyAxisInfoList = list;
        this.multiplyAxisWeightTotal = weightTotal;
        log.info("倍数轴配置加载完成，共{}条，总权重={}", list.size(), weightTotal);
    }

    // -------------------------------------------------------------------------
    // 游戏时动态生成第四轴（由 AbstractGaraGemstone3GameManager.normal() 调用）
    // -------------------------------------------------------------------------

    /**
     * 根据权重随机选出倍数轴符号，生成 [上格, 中格, 下格] 三个图标，
     * 将其追加到 lib.iconArr 末尾，并在 lib 中记录倍数值和奖池ID。
     * 供 AbstractGaraGemstone3GameManager.normal() 在游戏时调用。
     */
    public void generateAxisIcons(GaraGemstone3ResultLib lib) {
        int[] originalArr = lib.getIconArr();
//        int[] extended = appendMultiplyAxisIcons(originalArr, lib);
        lib.setIconArr(originalArr);
    }

    /**
     * 计算连线总倍数（外部可调用）。
     */
    public long calLineTimes(List<GaraGemstone3AwardLineInfo> list) {
        return CollUtil.isEmpty(list)
                ? 0
                : list.stream()
                .mapToInt(GaraGemstone3AwardLineInfo::getTotalTimes)
                .sum();
    }

    // -------------------------------------------------------------------------
    // 内部方法
    // -------------------------------------------------------------------------

    /**
     * 根据权重随机选出倍数轴符号，从 BaseRoller 20550114 中取连续3格图标（前、中、后），
     * 将其追加到3×3图标数组末尾，并在 lib 中记录倍数值和奖池ID。
     *
     * @param originalArr 原始3×3图标数组（长度10，index 0 不用）
     * @param lib         当前结果库（写入 multiplyAxisTimes / axisJackpotId）
     * @return 扩展后的数组（长度13，index 10/11/12 为第四轴上/中/下）
     */
    private int[] appendMultiplyAxisIcons(int[] originalArr, GaraGemstone3ResultLib lib) {
        // --- 1. 加权随机选出倍数轴符号 ---
        GaraGemstone3MultiplyAxisInfo selectedInfo;
        if (lib.getLibTypeSet() != null && lib.getLibTypeSet().contains(GaraGemstone3Constant.SpecialMode.JACKPOOL)) {
            selectedInfo = new GaraGemstone3MultiplyAxisInfo();
            selectedInfo.setTimes(0);
            selectedInfo.setIconId(GaraGemstone3Constant.BaseElement.ID_JACKPOOL);
        } else {
            selectedInfo = randomSelectAxisInfo();
            if (selectedInfo == null) {
                lib.setMultiplyAxisTimes(1);
                int[] newArr = new int[originalArr.length + 3];
                System.arraycopy(originalArr, 0, newArr, 0, originalArr.length);
                return newArr;
            }
        }
        // --- 2. 记录倍数（奖金符号倍数取1，奖池奖励另外处理）---
        long axisMultiplyTimes = selectedInfo.getTimes() > 0 ? selectedInfo.getTimes() : 1;
        lib.setMultiplyAxisTimes(axisMultiplyTimes);

        // --- 3. 若是奖金符号（times==0），从 prizePoolIdList 中匹配 truePool==iconId 的奖池 ---
        if (selectedInfo.getTimes() == 0) {
            BaseInitCfg initCfg = GameDataManager.getBaseInitCfg(this.gameType);
            if (initCfg != null && initCfg.getPrizePoolIdList() != null) {
                for (int poolId : initCfg.getPrizePoolIdList()) {
                    PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                    if (poolCfg != null) {
                        lib.setJackpotId(poolId);
                        lib.addJackpotId(poolId);
                        break;
                    }
                }
            }
        }

        // --- 4. 从 BaseRoller 20550114 中找到该符号的位置，取前/中/后3格 ---
        BaseRollerCfg rollerCfg = GameDataManager.getBaseRollerCfg(GaraGemstone3Constant.BaseRollerGroup.MULTIPLY_AXIS_ROLLER_ID);
        if (rollerCfg == null || rollerCfg.getElements() == null || rollerCfg.getElements().isEmpty()) {
            log.warn("倍数轴滚轴配置为空 rollerId={}", GaraGemstone3Constant.BaseRollerGroup.MULTIPLY_AXIS_ROLLER_ID);
            int[] newArr = new int[originalArr.length + 3];
            System.arraycopy(originalArr, 0, newArr, 0, originalArr.length);
            newArr[originalArr.length] = selectedInfo.getIconId();
            newArr[originalArr.length + 1] = selectedInfo.getIconId();
            newArr[originalArr.length + 2] = selectedInfo.getIconId();
            return newArr;
        }

        List<Integer> elements = rollerCfg.getElements();
        int size = elements.size();

        int centerPos;
        boolean isJackpool = selectedInfo.getIconId() == GaraGemstone3Constant.BaseElement.ID_JACKPOOL;
        if (isJackpool) {
            //JACKPOOL 的 icon=9 是代码合成的"虚拟符号"，roller elements 里本来就不存在，
            //直接随机取一个中心位置（前/后两格仍从 roller 取真符号），最后由后面的代码强写中格为 9
            int first = rollerCfg.getAxleCountScope() != null && rollerCfg.getAxleCountScope().size() >= 2
                    ? rollerCfg.getAxleCountScope().get(0) - 1 : 0;
            int last = rollerCfg.getAxleCountScope() != null && rollerCfg.getAxleCountScope().size() >= 2
                    ? rollerCfg.getAxleCountScope().get(1) - 1 : size - 1;
            centerPos = RandomUtils.randomMinMax(first, last);
        } else {
            // 找出目标符号在 elements 中所有出现的位置
            List<Integer> positions = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                if (elements.get(i).equals(selectedInfo.getIconId())) {
                    positions.add(i);
                }
            }

            if (positions.isEmpty()) {
                log.warn("倍数轴滚轴中未找到符号 iconId={}", selectedInfo.getIconId());
                int first = rollerCfg.getAxleCountScope() != null && rollerCfg.getAxleCountScope().size() >= 2
                        ? rollerCfg.getAxleCountScope().get(0) - 1 : 0;
                int last = rollerCfg.getAxleCountScope() != null && rollerCfg.getAxleCountScope().size() >= 2
                        ? rollerCfg.getAxleCountScope().get(1) - 1 : size - 1;
                centerPos = RandomUtils.randomMinMax(first, last);
            } else {
                centerPos = positions.get(RandomUtils.nextInt(positions.size()));
            }
        }

        // 取前、中、后（首尾相连）
        int prevPos = (centerPos - 1 + size) % size;
        int nextPos = (centerPos + 1) % size;
        int prevIcon = elements.get(prevPos);
        int centerIcon = elements.get(centerPos);
        int nextIcon = elements.get(nextPos);

        // --- 5. 构建扩展数组（第四轴 index 10=上格, 11=中格/选定, 12=下格）---
        int[] newArr = new int[originalArr.length + 3];
        System.arraycopy(originalArr, 0, newArr, 0, originalArr.length);
        newArr[originalArr.length] = prevIcon;    // index 10: 第四轴上格
        newArr[originalArr.length + 1] = centerIcon;  // index 11: 第四轴中格（倍数选定格）
        newArr[originalArr.length + 2] = nextIcon;    // index 12: 第四轴下格

        if (selectedInfo.getIconId() == GaraGemstone3Constant.BaseElement.ID_JACKPOOL) {
            newArr[originalArr.length + 1] = GaraGemstone3Constant.BaseElement.ID_JACKPOOL;  // index 11: 第四轴中格（倍数选定格）
        }

        log.debug("倍数轴生成完毕 iconId={} times={} axisJackpotId={} axisIcons=[{},{},{}]",
                selectedInfo.getIconId(), axisMultiplyTimes, lib.getJackpotId(),
                prevIcon, centerIcon, nextIcon);


        return newArr;
    }

    /**
     * 按权重随机选出一条倍数轴配置。
     */
    private GaraGemstone3MultiplyAxisInfo randomSelectAxisInfo() {
        if (CollUtil.isEmpty(multiplyAxisInfoList) || multiplyAxisWeightTotal <= 0) {
            return null;
        }
        int rand = RandomUtils.nextInt(multiplyAxisWeightTotal);
        int cumulative = 0;
        for (GaraGemstone3MultiplyAxisInfo info : multiplyAxisInfoList) {
            cumulative += info.getWeight();
            if (rand < cumulative) {
                return info;
            }
        }
        return multiplyAxisInfoList.get(multiplyAxisInfoList.size() - 1);
    }

    /**
     * 加载多格图标替换配置。
     * 5055022：多格图标替换个数_权重，如 0_2000|1_6000|...
     * 5055021：元素ID_图标倍数_权重，如 16_2_3000|24_3_1500|...
     */
    private void loadExpandConfig() {
        SpecialPlayCfg numCfg = GameDataManager.getSpecialPlayCfg(GaraGemstone3Constant.BaseRollerGroup.EXPAND_NUM_AXIS_CFG_ID);
        if (numCfg != null && !StringUtil.isEmpty(numCfg.getValue())) {
            List<int[]> list = new ArrayList<>();
            int total = 0;
            for (String entry : numCfg.getValue().split("\\|")) {
                String[] parts = entry.split("_");
                if (parts.length < 2) continue;
                int count = Integer.parseInt(parts[0]);
                int weight = Integer.parseInt(parts[1]);
                if (weight > 0) {
                    list.add(new int[]{count, weight});
                    total += weight;
                }
            }
            this.expandNumList = list;
            this.expandNumWeightTotal = total;
            log.info("多格图标替换个数配置加载完成，共{}条，总权重={}", list.size(), total);
        }

        SpecialPlayCfg iconCfg = GameDataManager.getSpecialPlayCfg(GaraGemstone3Constant.BaseRollerGroup.EXPAND_MULTIPLY_AXIS_CFG_ID);
        if (iconCfg != null && !StringUtil.isEmpty(iconCfg.getValue())) {
            List<int[]> list = new ArrayList<>();
            int total = 0;
            for (String entry : iconCfg.getValue().split("\\|")) {
                String[] parts = entry.split("_");
                if (parts.length < 3) continue;
                int elementId = Integer.parseInt(parts[0]);
                int multiplier = Integer.parseInt(parts[1]);
                int weight = Integer.parseInt(parts[2]);
                if (weight > 0) {
                    list.add(new int[]{elementId, multiplier, weight});
                    total += weight;
                }
            }
            this.expandIconList = list;
            this.expandIconWeightTotal = total;
            log.info("多格图标元素配置加载完成，共{}条，总权重={}", list.size(), total);
        }
    }

    /**
     * 在计算倍数前，随机将部分原始图标替换为多格图标（2倍/3倍图标），
     * 并对包含替换位置的中奖线应用对应倍数。
     *
     * <p>流程：
     * <ol>
     *   <li>从 5055022 加权随机出本次替换个数</li>
     *   <li>对每个替换：从 5055021 加权随机选出新图标，找对应原图标在主网格（1-9）的位置，
     *       最多重试 100 次；不可替换奖池图标（ID_JACKPOOL）</li>
     *   <li>对已替换位置所在的中奖线，将 baseTimes 乘以该图标倍数</li>
     * </ol>
     */
    private void expandMultiIcons(GaraGemstone3ResultLib lib, Map<Integer, int[]> presetReplacedMap) {
        int[] iconArr = lib.getIconArr();
        // position(1-9) → 该位置的替换倍数
        Map<Integer, Integer> replacedMap = new HashMap<>();
        // 把 GM 等预置的分裂图标位置先纳入，并把 iconArr 上的多格图标 ID 写回用于显示
        if (presetReplacedMap != null && !presetReplacedMap.isEmpty()) {
            for (Map.Entry<Integer, int[]> e : presetReplacedMap.entrySet()) {
                int pos = e.getKey();
                int[] meta = e.getValue(); // [multiIconId, multiplier]
                iconArr[pos] = meta[0];
                replacedMap.put(pos, meta[1]);
            }
        }

        if (CollUtil.isEmpty(expandNumList) || CollUtil.isEmpty(expandIconList)
                || expandNumWeightTotal <= 0 || expandIconWeightTotal <= 0) {
            applySplitTimes(lib, replacedMap);
            return;
        }

        // 1. 随机出本次替换个数
        int replaceCount = 0;
        {
            int rand = RandomUtils.nextInt(expandNumWeightTotal);
            int cumulative = 0;
            for (int[] entry : expandNumList) {
                cumulative += entry[1];
                if (rand < cumulative) {
                    replaceCount = entry[0];
                    break;
                }
            }
        }
        if (replaceCount <= 0) {
            applySplitTimes(lib, replacedMap);
            return;
        }

        // 2. 逐个随机替换图标
        for (int i = 0; i < replaceCount; i++) {
            boolean replaced = false;
            for (int attempt = 0; attempt < 100; attempt++) {
                // 从 5055021 加权随机选出一个多格图标配置
                int rand = RandomUtils.nextInt(expandIconWeightTotal);
                int cumulative = 0;
                int[] selectedEntry = null;
                for (int[] entry : expandIconList) {
                    cumulative += entry[2];
                    if (rand < cumulative) {
                        selectedEntry = entry;
                        break;
                    }
                }
                if (selectedEntry == null) continue;

                int newIconId = selectedEntry[0];
                int multiplier = selectedEntry[1];
                Integer originalIconId = MULTI_TO_ORIGINAL_MAP.get(newIconId);
                if (originalIconId == null) continue;

                // 查找主网格（1-9）中符合条件的原图标位置（排除jackpool、已替换位置）
                List<Integer> validPositions = new ArrayList<>();
                for (int pos = 1; pos <= 9; pos++) {
                    if (iconArr[pos] == originalIconId
                            && iconArr[pos] != GaraGemstone3Constant.BaseElement.ID_JACKPOOL
                            && !replacedMap.containsKey(pos)) {
                        validPositions.add(pos);
                    }
                }
                if (validPositions.isEmpty()) {
                    continue; // 没有符合的原图标，重新随机
                }

                int pos = validPositions.get(RandomUtils.nextInt(validPositions.size()));
                iconArr[pos] = newIconId;
                replacedMap.put(pos, multiplier);
                replaced = true;
                break;
            }
            if (!replaced) {
                log.debug("100次循环未找到可替换的图标，放弃此次替换 loopIndex={}", i);
            }
        }

        lib.setIconArr(iconArr);
        applySplitTimes(lib, replacedMap);
    }

    /**
     * 对包含替换位置的中奖线累乘分裂倍数（同一线多个分裂图标则累乘）。
     */
    private void applySplitTimes(GaraGemstone3ResultLib lib, Map<Integer, Integer> replacedMap) {
        if (replacedMap == null || replacedMap.isEmpty()) {
            return;
        }
        List<GaraGemstone3AwardLineInfo> lines = lib.getAwardLineInfoList();
        if (CollUtil.isEmpty(lines)) {
            return;
        }
        for (GaraGemstone3AwardLineInfo lineInfo : lines) {
            BaseLineCfg lineCfg = findBaseLineCfgByLineId(lineInfo.getId());
            if (lineCfg == null || CollUtil.isEmpty(lineCfg.getPosLocation())) {
                continue;
            }
            List<Integer> posLocations = lineCfg.getPosLocation();
            int checkCount = Math.min(lineInfo.getSameCount(), posLocations.size());
            int splitTimes = 1;
            for (int j = 0; j < checkCount; j++) {
                Integer mult = replacedMap.get(posLocations.get(j));
                if (mult != null) {
                    splitTimes *= mult; // 累乘：同一线2x+3x → splitTimes=6
                }
            }
            if (splitTimes > 1) {
                lineInfo.setSplitTimes(splitTimes);
            }
        }
    }

    /**
     * 扫描 iconArr 中已存在的分裂图标（如 GM setIcons 直接放置的 28/24 等），
     * 将其位置/倍数记录下来，并把对应位置临时还原成原图标，供 winLines 正常匹配连线。
     * 返回 pos → [multiIconId, multiplier]，供 expandMultiIcons 写回 iconArr 和应用 splitTimes。
     */
    private Map<Integer, int[]> revertPreplacedMultiIcons(GaraGemstone3ResultLib lib) {
        Map<Integer, int[]> presetMap = new HashMap<>();
        int[] iconArr = lib.getIconArr();
        if (iconArr == null || CollUtil.isEmpty(expandIconList)) {
            return presetMap;
        }
        int upper = Math.min(9, iconArr.length - 1);
        for (int pos = 1; pos <= upper; pos++) {
            int iconId = iconArr[pos];
            Integer originalIconId = MULTI_TO_ORIGINAL_MAP.get(iconId);
            if (originalIconId == null || originalIconId == iconId) {
                continue;
            }
            int multiplier = 0;
            for (int[] entry : expandIconList) {
                if (entry[0] == iconId) {
                    multiplier = entry[1];
                    break;
                }
            }
            if (multiplier <= 1) {
                continue;
            }
            presetMap.put(pos, new int[]{iconId, multiplier});
            iconArr[pos] = originalIconId;
        }
        return presetMap;
    }

    /**
     * 根据 lineId 查找 BaseLineCfg。父类 baseLineCfgMap 结构为 gameMode→lineId→cfg。
     * 优先用 gameMode=0（通用）的，再回退 gameMode=1（普通模式）。
     */
    private BaseLineCfg findBaseLineCfgByLineId(int lineId) {
        if (this.baseLineCfgMap == null) {
            return null;
        }
        Map<Integer, BaseLineCfg> map = this.baseLineCfgMap.get(0);
        if (map == null || map.isEmpty()) {
            map = this.baseLineCfgMap.get(1);
        }
        return map == null ? null : map.get(lineId);
    }

    public GaraGemstone3ResultLib checkAward(int[] arr, GaraGemstone3ResultLib lib, boolean freeModel) throws Exception {

        lib.setGameType(this.gameType);

        int newLength = arr.length - 3;
        int[] newArr = new int[newLength];
        System.arraycopy(arr, 0, newArr, 0, newLength);
        int[] extended = appendMultiplyAxisIcons(newArr, lib);
        lib.setIconArr(extended);

        //预置分裂图标（如 GM setIcons 直接放置的 28/24 等）先还原为原图标，供 winLines 正常连线
        Map<Integer, int[]> presetMultiMap = revertPreplacedMultiIcons(lib);

        //检查连线
        List<GaraGemstone3AwardLineInfo> awardLineInfoList = winLines(lib, freeModel);
        lib.setAwardLineInfoList(awardLineInfoList);

        //检查指定图案
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = assignPattern(lib);
        lib.addSpecialAuxiliaryInfo(specialAuxiliaryInfoList);

        //检查满线图案_x连
        List<GaraGemstone3AwardLineInfo> fullLineInfoList = fullLine(lib);
        lib.addAllAwardLineInfo(fullLineInfoList);

        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        //检查满线图案_数量
        List<GaraGemstone3AwardLineInfo> fullLineCountInfoList = fullLineCount(lib);
        lib.addAllAwardLineInfo(fullLineCountInfoList);

        //检查连线分散数量
        List<GaraGemstone3AwardLineInfo> lineDispersionCount = lineDispersionCount(lib);
        lib.addAllAwardLineInfo(lineDispersionCount);

        //多格图标替换，修改图标并对对应中奖线应用分裂倍数
        expandMultiIcons(lib, presetMultiMap);

        //计算倍数
        calTimes(lib);

        return lib;
    }

    @Override
    public void calTimes(GaraGemstone3ResultLib lib) throws Exception {
        List<GaraGemstone3AwardLineInfo> lines = lib.getAwardLineInfoList();
        if (CollUtil.isEmpty(lines)) {
            return;
        }
        long lineTotal = 0;
        for (GaraGemstone3AwardLineInfo lineInfo : lines) {
            int total = lineInfo.getBaseTimes() * lineInfo.getSplitTimes();
            lineInfo.setTotalTimes(total);
            lineTotal += total;
        }
        //第4列中间图标 倍数 * 各中奖线 totalTimes 之和
        lib.addTimes(lib.getMultiplyAxisTimes() * lineTotal);
    }
}