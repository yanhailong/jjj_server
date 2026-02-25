package com.jjg.game.slots.data;

import com.jjg.game.sampledata.bean.SpecialResultLibCfg;

import java.util.List;
import java.util.Map;

/**
 * specialResultLib表分析计算后的缓存对象
 * @author 11
 * @date 2025/7/25 9:28
 */
public class SpecialResultLibCacheData {
    private int defaultRewardSectionIndex = -1;
    //modelId -> cfg
    private Map<Integer, SpecialResultLibCfg> resultLibMap;
//    specialResultLib表中typeProp字段的随机权重信息
    private Map<Integer, List<TypePropData>> resultLibTypePropInfoMap;
    //specialResultLib表中section字段的每个倍数的随机权重信息  modelId -> tpyeId -> PropInfo
    private Map<Integer, Map<Integer,PropInfo>> resultLibSectionPropMap;
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

    public Map<Integer, List<TypePropData>> getResultLibTypePropInfoMap() {
        return resultLibTypePropInfoMap;
    }

    public void setResultLibTypePropInfoMap(Map<Integer, List<TypePropData>> resultLibTypePropInfoMap) {
        this.resultLibTypePropInfoMap = resultLibTypePropInfoMap;
    }

    public Map<Integer, Map<Integer, PropInfo>> getResultLibSectionPropMap() {
        return resultLibSectionPropMap;
    }

    public void setResultLibSectionPropMap(Map<Integer, Map<Integer, PropInfo>> resultLibSectionPropMap) {
        this.resultLibSectionPropMap = resultLibSectionPropMap;
    }

    public Map<Integer, Map<Integer, int[]>> getResultLibSectionMap() {
        return resultLibSectionMap;
    }

    public void setResultLibSectionMap(Map<Integer, Map<Integer, int[]>> resultLibSectionMap) {
        this.resultLibSectionMap = resultLibSectionMap;
    }

    /**
     * 获取specialResultLib中的typeProp信息
     * @param modelId
     * @param betValue
     * @return
     */
    public TypePropData getTypePropData(int modelId,long betValue){
        if(this.resultLibTypePropInfoMap == null || this.resultLibTypePropInfoMap.isEmpty()){
            return null;
        }

        List<TypePropData> dataList = this.resultLibTypePropInfoMap.get(modelId);
        if(dataList == null || dataList.isEmpty()){
            return null;
        }

        for(TypePropData data : dataList){
            if(betValue >= data.getBegin() && betValue < data.getEnd()){
                return data;
            }
        }
        return null;
    }
}
