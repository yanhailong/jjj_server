package com.jjg.game.slots.game.garaGemstone1.manager;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRollerCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.garaGemstone1.GaraGemstone1Constant;
import com.jjg.game.slots.game.garaGemstone1.data.GaraGemstone1AwardLineInfo;
import com.jjg.game.slots.game.garaGemstone1.data.GaraGemstone1MultiplyAxisInfo;
import com.jjg.game.slots.game.garaGemstone1.data.GaraGemstone1ResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import jodd.util.StringUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GaraGemstone1GenerateManager extends AbstractSlotsGenerateManager<GaraGemstone1AwardLineInfo, GaraGemstone1ResultLib> {
    public GaraGemstone1GenerateManager() {
        super(GaraGemstone1ResultLib.class);
    }

    /**
     * 倍数轴配置列表，从 SpecialPlay 5055011 加载
     */
    private List<GaraGemstone1MultiplyAxisInfo> multiplyAxisInfoList;
    /**
     * 倍数轴权重总和，用于加权随机
     */
    private int multiplyAxisWeightTotal;

    @Override
    protected GaraGemstone1AwardLineInfo getAwardLineInfo() {
        return new GaraGemstone1AwardLineInfo();
    }

    @Override
    protected void specialPlayConfig() {
        loadMultiplyAxisConfig();
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
        SpecialPlayCfg cfg = GameDataManager.getSpecialPlayCfg(GaraGemstone1Constant.BaseRollerGroup.MULTIPLY_AXIS_CFG_ID);
        if (cfg == null || StringUtil.isEmpty(cfg.getValue())) {
            log.warn("倍数轴配置为空，gameType={}, cfgId={}", this.gameType, GaraGemstone1Constant.BaseRollerGroup.MULTIPLY_AXIS_CFG_ID);
            return;
        }
        String[] entries = cfg.getValue().split("\\|");
        List<GaraGemstone1MultiplyAxisInfo> list = new ArrayList<>(entries.length);
        int weightTotal = 0;
        for (String entry : entries) {
            String[] parts = entry.split("_");
            if (parts.length < 3) {
                log.warn("倍数轴配置格式错误，跳过：{}", entry);
                continue;
            }
            GaraGemstone1MultiplyAxisInfo info = new GaraGemstone1MultiplyAxisInfo();
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
    // 游戏时动态生成第四轴（由 AbstractGaraGemstone1GameManager.normal() 调用）
    // -------------------------------------------------------------------------

    /**
     * 根据权重随机选出倍数轴符号，生成 [上格, 中格, 下格] 三个图标，
     * 将其追加到 lib.iconArr 末尾，并在 lib 中记录倍数值和奖池ID。
     * 供 AbstractGaraGemstone1GameManager.normal() 在游戏时调用。
     */
    public void generateAxisIcons(GaraGemstone1ResultLib lib) {
        int[] originalArr = lib.getIconArr();
//        int[] extended = appendMultiplyAxisIcons(originalArr, lib);
        lib.setIconArr(originalArr);
    }

    /**
     * 计算连线总倍数（外部可调用）。
     */
    public long calLineTimes(List<GaraGemstone1AwardLineInfo> list) {
        return CollUtil.isEmpty(list)
                ? 0
                : list.stream()
                .mapToInt(GaraGemstone1AwardLineInfo::getBaseTimes)
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
    private int[] appendMultiplyAxisIcons(int[] originalArr, GaraGemstone1ResultLib lib) {
        // --- 1. 加权随机选出倍数轴符号 ---
        GaraGemstone1MultiplyAxisInfo selectedInfo;
        if (lib.getLibTypeSet() != null && lib.getLibTypeSet().contains(GaraGemstone1Constant.SpecialMode.JACKPOOL)) {
            selectedInfo = new GaraGemstone1MultiplyAxisInfo();
            selectedInfo.setTimes(0);
            selectedInfo.setIconId(GaraGemstone1Constant.BaseElement.ID_JACKPOOL);
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
        BaseRollerCfg rollerCfg = GameDataManager.getBaseRollerCfg(GaraGemstone1Constant.BaseRollerGroup.MULTIPLY_AXIS_ROLLER_ID);
        if (rollerCfg == null || rollerCfg.getElements() == null || rollerCfg.getElements().isEmpty()) {
            log.warn("倍数轴滚轴配置为空 rollerId={}", GaraGemstone1Constant.BaseRollerGroup.MULTIPLY_AXIS_ROLLER_ID);
            int[] newArr = new int[originalArr.length + 3];
            System.arraycopy(originalArr, 0, newArr, 0, originalArr.length);
            newArr[originalArr.length] = selectedInfo.getIconId();
            newArr[originalArr.length + 1] = selectedInfo.getIconId();
            newArr[originalArr.length + 2] = selectedInfo.getIconId();
            return newArr;
        }

        List<Integer> elements = rollerCfg.getElements();
        int size = elements.size();

        // 找出目标符号在 elements 中所有出现的位置
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (elements.get(i).equals(selectedInfo.getIconId())) {
                positions.add(i);
            }
        }

        int centerPos;
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

        if (selectedInfo.getIconId() == GaraGemstone1Constant.BaseElement.ID_JACKPOOL) {
            newArr[originalArr.length + 1] = GaraGemstone1Constant.BaseElement.ID_JACKPOOL;  // index 11: 第四轴中格（倍数选定格）
        }

        log.debug("倍数轴生成完毕 iconId={} times={} axisJackpotId={} axisIcons=[{},{},{}]",
                selectedInfo.getIconId(), axisMultiplyTimes, lib.getJackpotId(),
                prevIcon, centerIcon, nextIcon);
        return newArr;
    }

    /**
     * 按权重随机选出一条倍数轴配置。
     */
    private GaraGemstone1MultiplyAxisInfo randomSelectAxisInfo() {
        if (CollUtil.isEmpty(multiplyAxisInfoList) || multiplyAxisWeightTotal <= 0) {
            return null;
        }
        int rand = RandomUtils.nextInt(multiplyAxisWeightTotal);
        int cumulative = 0;
        for (GaraGemstone1MultiplyAxisInfo info : multiplyAxisInfoList) {
            cumulative += info.getWeight();
            if (rand < cumulative) {
                return info;
            }
        }
        return multiplyAxisInfoList.get(multiplyAxisInfoList.size() - 1);
    }


    public GaraGemstone1ResultLib checkAward(int[] arr, GaraGemstone1ResultLib lib, boolean freeModel) throws Exception {

        lib.setGameType(this.gameType);

        int newLength = arr.length - 3;
        int[] newArr = new int[newLength];
        System.arraycopy(arr, 0, newArr, 0, newLength);
        int[] extended = appendMultiplyAxisIcons(newArr, lib);
        lib.setIconArr(extended);

        //检查连线
        List<GaraGemstone1AwardLineInfo> awardLineInfoList = winLines(lib, freeModel);
        lib.setAwardLineInfoList(awardLineInfoList);

        //检查指定图案
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = assignPattern(lib);
        lib.addSpecialAuxiliaryInfo(specialAuxiliaryInfoList);

        //检查满线图案_x连
        List<GaraGemstone1AwardLineInfo> fullLineInfoList = fullLine(lib);
        lib.addAllAwardLineInfo(fullLineInfoList);

        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        //检查满线图案_数量
        List<GaraGemstone1AwardLineInfo> fullLineCountInfoList = fullLineCount(lib);
        lib.addAllAwardLineInfo(fullLineCountInfoList);

        //检查连线分散数量
        List<GaraGemstone1AwardLineInfo> lineDispersionCount = lineDispersionCount(lib);
        lib.addAllAwardLineInfo(lineDispersionCount);

        //计算倍数
        calTimes(lib);

        return lib;
    }

    @Override
    public void calTimes(GaraGemstone1ResultLib lib) throws Exception {
        //第4列中间图标 倍数 * 中奖线
        lib.addTimes(lib.getMultiplyAxisTimes() * calLineTimes(lib.getAwardLineInfoList()));
    }
}