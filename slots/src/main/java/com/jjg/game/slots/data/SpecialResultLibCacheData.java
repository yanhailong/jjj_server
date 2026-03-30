package com.jjg.game.slots.data;

import com.jjg.game.sampledata.bean.SpecialResultLibCfg;

import java.util.List;
import java.util.Map;

/**
 * specialResultLib表分析计算后的缓存对象
 *
 * @author 11
 * @date 2025/7/25 9:28
 */
public class SpecialResultLibCacheData {
    private int defaultRewardSectionIndex = -1;
    //modelId -> cfg
    private Map<Integer, SpecialResultLibCfg> resultLibMap;
    //specialResultLib表中typeProp字段的随机权重信息 specialResultLib.modelId -> propInfo
    private Map<Integer, PropInfo> resultLibTypePropInfoMap;
    //specialResultLib表中typeProp字段的随机权重信息(排除了jackpot类型) specialResultLib.modelId -> propInfo
    private Map<Integer, PropInfo> noJackpotResultLibTypePropInfoMap;
    //specialResultLib表中section字段的每个倍数的随机权重信息  modelId -> tpyeId -> PropInfo
    private Map<Integer, Map<Integer,PropInfo>> resultLibSectionPropMap;
    //specialResultLib表中section字段的每个倍数的随机权重信息  modelId -> tpyeId -> PropInfo
    private Map<Integer, List<ChangeSectionData>> changeResultLibSectionPropMap;
    //specialResultLib表中section字段的倍数区间  modelId -> tpyeId -> 下标id -> 倍数区间
    private Map<Integer,Map<Integer,int[]>> resultLibSectionMap;

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

    /**
     * 根据调控序列id和下注金额，找到对应倍数区间概率
     * @param modelId
     * @param betValue
     * @return
     */
    public Map<Integer, PropInfo> getPropMap(int modelId,long betValue){
        //先去检查是否有被修改的概率
        if(this.changeResultLibSectionPropMap != null && !this.changeResultLibSectionPropMap.isEmpty()){
            List<ChangeSectionData> tmpList = this.changeResultLibSectionPropMap.get(modelId);
            if(tmpList != null && !tmpList.isEmpty()){
                ChangeSectionData data = tmpList.stream().filter(d -> betValue >= d.getBetMin() && betValue < d.getBetMax()).findFirst().orElse(null);
                if(data != null){
                    return data.getSectionPropMap();
                }
            }
        }
        return this.resultLibSectionPropMap.get(modelId);
    }
}
