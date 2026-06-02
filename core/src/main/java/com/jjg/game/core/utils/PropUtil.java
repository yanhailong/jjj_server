package com.jjg.game.core.utils;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.PropInfo;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2026/6/2
 */
public class PropUtil {
    /**
     * 将 <值,权重>格式的map转化为PropInfo
     * @param map
     * @return
     */
    public static PropInfo converMapToPropInfo(Map<Integer, Integer> map) {
        if(map == null || map.isEmpty()) {
            return null;
        }

        int begin = 0;
        int end = 0;

        PropInfo propInfo = new PropInfo();
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            begin = end;
            end += entry.getValue();
            propInfo.addProp(entry.getKey(), begin, end);
        }
        return propInfo;
    }

    /**
     * 将 <值,权重,最大次数限制>格式的list转化为PropInfo
     * @param list
     * @return
     */
    public static PropInfo converMapToLimitPropInfo(List<List<Integer>> list) {
        if(list == null || list.isEmpty()) {
            return null;
        }

        int begin = 0;
        int end = 0;

        PropInfo propInfo = new PropInfo();
        for (List<Integer> l : list) {
            int prop = l.get(1);
            int maxShowLimit = l.get(2);

            begin = end;
            end += prop;
            propInfo.addProp(l.get(0), begin, end,maxShowLimit);
        }
        return propInfo;
    }

    /**
     * 根据万分比返回all的值
     * @param prop
     * @param all
     * @return
     */
    public static int calProp(int prop,int all) {
        if(prop == 0) {
            return 0;
        }

        if(prop == GameConstant.TEN_THOUSAND) {
            return all;
        }
        BigDecimal propValue = BigDecimal.valueOf(prop);
        BigDecimal divide = propValue.divide(GameConstant.TEN_THOUSAND_BD, 4, BigDecimal.ROUND_HALF_UP);
        BigDecimal multiply = BigDecimal.valueOf(all).multiply(divide);
        return multiply.intValue();
    }

    /**
     * 根据万分比返回all的值
     * @param prop
     * @param all
     * @return
     */
    public static long calProp(int prop,long all) {
        if(prop == 0) {
            return 0;
        }

        if(prop == GameConstant.TEN_THOUSAND) {
            return all;
        }
        BigDecimal propValue = BigDecimal.valueOf(prop);
        BigDecimal divide = propValue.divide(GameConstant.TEN_THOUSAND_BD, 4, BigDecimal.ROUND_HALF_UP);
        BigDecimal multiply = BigDecimal.valueOf(all).multiply(divide);
        return multiply.longValue();
    }

    /**
     * 根据万分比计算是否命中
     * @param prop
     * @return
     */
    public static boolean calProp(int prop) {
        if(prop == 0) {
            return false;
        }

        if(prop == GameConstant.TEN_THOUSAND) {
            return true;
        }

        int rand = RandomUtils.randomMinMax(1,GameConstant.TEN_THOUSAND);
        return rand <= prop;
    }


    /**
     * 克隆 PropInfo,对其 propMap 中匹配 key 的权重累加 delta,重算 [begin,end) 与 sum
     */
    public static PropInfo applyPropInfoDelta(PropInfo propInfo, Map<Integer, Integer> deltaMap) {
        if (deltaMap.isEmpty()) {
            return propInfo;
        }

        PropInfo cloned = propInfo.clone();
        //保留原顺序读取每个 key 的权重
        Map<Integer, Integer> weightMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, int[]> en : cloned.getPropMap().entrySet()) {
            int[] range = en.getValue();
            weightMap.put(en.getKey(), range[1] - range[0]);
        }

        boolean changed = false;
        for (Map.Entry<Integer, Integer> en : deltaMap.entrySet()) {
            Integer key = en.getKey();
            if (!weightMap.containsKey(key)) {
                continue;
            }
            int newWeight = Math.max(0, weightMap.get(key) + en.getValue());
            weightMap.put(key, newWeight);
            changed = true;
        }
        if (!changed) {
            return propInfo;
        }

        int begin = 0;
        int sum = 0;
        for (Map.Entry<Integer, Integer> en : weightMap.entrySet()) {
            int weight = en.getValue();
            int[] range = cloned.getPropMap().get(en.getKey());
            range[0] = begin;
            range[1] = begin + weight;
            begin = range[1];
            sum += weight;
        }
        cloned.setSum(sum);
        return cloned;
    }

    private static final int tenThousand = 10000;

    /**
     * 计算baseNum的万分比的值
     *
     * @param prop
     * @param baseNum
     * @return
     */
    public static int propBase(int prop, int baseNum) {
        if(prop < 1){
            return 0;
        }

        if(prop >= tenThousand){
            return baseNum;
        }

        return (int) ((long) baseNum * prop / tenThousand);
    }
}
