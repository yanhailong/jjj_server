package com.jjg.game.slots.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储weightChange的权重变更指令（delta），查询时按需计算
 *
 * @author 11
 * @date 2026/3/3
 */
public class ChangeSectionData {
    //下注下限
    private long betMin;
    //下注上限
    private long betMax;
    //权重变更指令: libType -> (sectionIndex -> newWeight)
    private Map<Integer, Map<Integer, Integer>> weightChanges;

    public long getBetMin() {
        return betMin;
    }

    public void setBetMin(long betMin) {
        this.betMin = betMin;
    }

    public long getBetMax() {
        return betMax;
    }

    public void setBetMax(long betMax) {
        this.betMax = betMax;
    }

    public Map<Integer, Map<Integer, Integer>> getWeightChanges() {
        return weightChanges;
    }

    public void setWeightChanges(Map<Integer, Map<Integer, Integer>> weightChanges) {
        this.weightChanges = weightChanges;
    }

    /**
     * 将变更指令应用到基础权重map上，返回修改后的拷贝
     *
     * @param baseSectionPropMap 原始 typeSectionPropMap
     * @return 应用变更后的map
     */
    public Map<Integer, PropInfo> applyTo(Map<Integer, PropInfo> baseSectionPropMap) {
        Map<Integer, PropInfo> result = new HashMap<>(baseSectionPropMap);
        for (Map.Entry<Integer, Map<Integer, Integer>> en : weightChanges.entrySet()) {
            int libType = en.getKey();
            PropInfo base = baseSectionPropMap.get(libType);
            if (base == null) {
                continue;
            }

            PropInfo propInfo = base.clone();
            for (Map.Entry<Integer, Integer> change : en.getValue().entrySet()) {
                int[] range = propInfo.getPropMap().get(change.getKey());
                if (range != null) {
                    range[1] = range[0] + change.getValue();
                }
            }

            //移除权重为0的entry，重新计算begin/end区间和sum
            propInfo.getPropMap().entrySet().removeIf(entry -> entry.getValue()[1] - entry.getValue()[0] <= 0);
            int begin = 0;
            for (Map.Entry<Integer, int[]> entry : propInfo.getPropMap().entrySet()) {
                int[] range = entry.getValue();
                int weight = range[1] - range[0];
                range[0] = begin;
                range[1] = begin + weight;
                begin = range[1];
            }
            propInfo.setSum(begin);
            result.put(libType, propInfo);
        }
        return result;
    }
}
