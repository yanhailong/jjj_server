package com.jjg.game.slots.data;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.PropInfo;
import com.jjg.game.sampledata.bean.SpecialResultLibCfg;
import com.jjg.game.slots.constant.SlotsConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * specialResultLib表分析计算后的缓存对象
 *
 * @author 11
 * @date 2025/7/25 9:28
 */
public class SpecialResultLibCacheData {
    private static final Logger log = LoggerFactory.getLogger(SpecialResultLibCacheData.class);
    private int defaultRewardSectionIndex = -1;
    //modelId -> cfg
    private Map<Integer, SpecialResultLibCfg> resultLibMap;
    //specialResultLib表中typeProp字段的随机权重信息 specialResultLib.modelId -> propInfo
    private Map<Integer, PropInfo> resultLibTypePropInfoMap;
    //specialResultLib表中typeProp字段的随机权重信息(排除了jackpot类型) specialResultLib.modelId -> propInfo
    private Map<Integer, PropInfo> noJackpotResultLibTypePropInfoMap;
    //specialResultLib表中section字段的每个倍数的随机权重信息  modelId -> tpyeId -> PropInfo
    private Map<Integer, Map<Integer, PropInfo>> resultLibSectionPropMap;
    //specialResultLib表中section字段的每个倍数的随机权重信息(根据weightChange修改)  modelId -> tpyeId -> PropInfo
    private Map<Integer, List<ChangeSectionData>> changeResultLibSectionPropMap;
    //specialResultLib表中section字段的倍数区间  modelId -> tpyeId -> 下标id -> 倍数区间
    private Map<Integer, Map<Integer, int[]>> resultLibSectionMap;
    //specialResultLib表中section字段的每个倍数的随机权重信息(根据mark修改)  modelId -> tpyeId -> PropInfo
    private Map<Integer, List<ChangeSectionData2>> markResultLibSectionPropMap;
    //specialResultLib表中section字段的每个倍数的随机权重信息(根据accumulate修改)  modelId -> tpyeId -> PropInfo
    private Map<Integer, List<ChangeSectionData2>> accumulateResultLibSectionPropMap;
    //specialResultLib表中section字段的每个倍数的随机权重信息(根据prizeless修改)  modelId -> tpyeId -> PropInfo
    private Map<Integer, List<ChangeSectionData2>> prizelessResultLibSectionPropMap;

    public int getDefaultRewardSectionIndex() {
        return defaultRewardSectionIndex;
    }

    public void setDefaultRewardSectionIndex(int defaultRewardSectionIndex) {
        this.defaultRewardSectionIndex = defaultRewardSectionIndex;
    }

    public Map<Integer, SpecialResultLibCfg> getResultLibMap() {
        return resultLibMap;
    }

    public void setResultLibMap(Map<Integer, SpecialResultLibCfg> resultLibMap) {
        this.resultLibMap = resultLibMap;
    }

    public Map<Integer, PropInfo> getResultLibTypePropInfoMap() {
        return resultLibTypePropInfoMap;
    }

    public void setResultLibTypePropInfoMap(Map<Integer, PropInfo> resultLibTypePropInfoMap) {
        this.resultLibTypePropInfoMap = resultLibTypePropInfoMap;
    }

    public Map<Integer, PropInfo> getNoJackpotResultLibTypePropInfoMap() {
        return noJackpotResultLibTypePropInfoMap;
    }

    public void setNoJackpotResultLibTypePropInfoMap(Map<Integer, PropInfo> noJackpotResultLibTypePropInfoMap) {
        this.noJackpotResultLibTypePropInfoMap = noJackpotResultLibTypePropInfoMap;
    }

    public Map<Integer, Map<Integer, PropInfo>> getResultLibSectionPropMap() {
        return resultLibSectionPropMap;
    }

    public void setResultLibSectionPropMap(Map<Integer, Map<Integer, PropInfo>> resultLibSectionPropMap) {
        this.resultLibSectionPropMap = resultLibSectionPropMap;
    }

    public Map<Integer, List<ChangeSectionData>> getChangeResultLibSectionPropMap() {
        return changeResultLibSectionPropMap;
    }

    public void setChangeResultLibSectionPropMap(Map<Integer, List<ChangeSectionData>> changeResultLibSectionPropMap) {
        this.changeResultLibSectionPropMap = changeResultLibSectionPropMap;
    }

    public Map<Integer, Map<Integer, int[]>> getResultLibSectionMap() {
        return resultLibSectionMap;
    }

    public void setResultLibSectionMap(Map<Integer, Map<Integer, int[]>> resultLibSectionMap) {
        this.resultLibSectionMap = resultLibSectionMap;
    }

    public Map<Integer, List<ChangeSectionData2>> getMarkResultLibSectionPropMap() {
        return markResultLibSectionPropMap;
    }

    public void setMarkResultLibSectionPropMap(Map<Integer, List<ChangeSectionData2>> markResultLibSectionPropMap) {
        this.markResultLibSectionPropMap = markResultLibSectionPropMap;
    }

    public Map<Integer, List<ChangeSectionData2>> getAccumulateResultLibSectionPropMap() {
        return accumulateResultLibSectionPropMap;
    }

    public void setAccumulateResultLibSectionPropMap(Map<Integer, List<ChangeSectionData2>> accumulateResultLibSectionPropMap) {
        this.accumulateResultLibSectionPropMap = accumulateResultLibSectionPropMap;
    }

    public Map<Integer, List<ChangeSectionData2>> getPrizelessResultLibSectionPropMap() {
        return prizelessResultLibSectionPropMap;
    }

    public void setPrizelessResultLibSectionPropMap(Map<Integer, List<ChangeSectionData2>> prizelessResultLibSectionPropMap) {
        this.prizelessResultLibSectionPropMap = prizelessResultLibSectionPropMap;
    }

    /**
     * 根据调控序列id和下注金额，找到对应倍数区间概率
     *
     * @param modelId
     * @param betValue
     * @param slotsVipType   vip类型，对应 specialResultLib表中 mark字段的类型
     * @param allBetCount    玩家累计下注次数
     * @param prizelessCount 玩家连续未中奖次数
     * @return
     */
    public Map<Integer, PropInfo> getPropMap(int modelId, long betValue, int slotsVipType, int allBetCount, int prizelessCount) {
        Map<Integer, PropInfo> basePropMap = this.resultLibSectionPropMap.get(SlotsConst.Common.DEFAULT_SPECIAL_RESULT_LIB_MODELID);

        log.warn("modelId: " + modelId + ",betValue: " + betValue + ",slotsVipType: " + slotsVipType + ",allBetCount: " + allBetCount + ",prizelessCount: " + prizelessCount);
        //1.判断 vip
        if (slotsVipType > 0 && this.markResultLibSectionPropMap != null && !this.markResultLibSectionPropMap.isEmpty()) {
            List<ChangeSectionData2> tmpList = this.markResultLibSectionPropMap.get(modelId);
            if (tmpList != null && !tmpList.isEmpty()) {
                ChangeSectionData2 data2 = tmpList.stream().filter(d -> slotsVipType == d.getType()).findFirst().orElse(null);
                if (data2 != null) {
                    log.warn("vip修改");
                    return data2.applyTo(basePropMap);
                }
            }
        }

        //2.玩家累计下注次数
        if (allBetCount >= 0 && this.accumulateResultLibSectionPropMap != null && !this.accumulateResultLibSectionPropMap.isEmpty()) {
            List<ChangeSectionData2> tmpList = this.accumulateResultLibSectionPropMap.get(modelId);
            if (tmpList != null && !tmpList.isEmpty()) {
                ChangeSectionData2 data2 = tmpList.stream().filter(d -> allBetCount < d.getType()).findFirst().orElse(null);
                if (data2 != null) {
                    log.warn("玩家累计下注次数 data2={}", JSONObject.toJSONString(data2));
                    return data2.applyTo(basePropMap);
                }
            }
        }

        //3.玩家连续未中奖次数
        if (prizelessCount > 0 && this.prizelessResultLibSectionPropMap != null && !this.prizelessResultLibSectionPropMap.isEmpty()) {
            List<ChangeSectionData2> tmpList = this.prizelessResultLibSectionPropMap.get(modelId);
            if (tmpList != null && !tmpList.isEmpty()) {
                ChangeSectionData2 data2 = tmpList.stream().filter(d -> prizelessCount == d.getType()).findFirst().orElse(null);
                if (data2 != null) {
                    log.warn("玩家连续未中奖次数");
                    return data2.applyTo(basePropMap);
                }
            }
        }

        //4.检查是否有被修改的概率
        if (this.changeResultLibSectionPropMap != null && !this.changeResultLibSectionPropMap.isEmpty()) {
            List<ChangeSectionData> tmpList = this.changeResultLibSectionPropMap.get(modelId);
            if (tmpList != null && !tmpList.isEmpty()) {
                ChangeSectionData data = tmpList.stream().filter(d -> betValue >= d.getBetMin() && betValue < d.getBetMax()).findFirst().orElse(null);
                if (data != null) {
                    log.warn("检查是否有被修改的概率");
                    return data.applyTo(basePropMap);
                }
            }
        }
        return basePropMap;
    }
}
