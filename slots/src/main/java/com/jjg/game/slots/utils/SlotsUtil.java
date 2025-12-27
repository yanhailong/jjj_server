package com.jjg.game.slots.utils;

import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.slots.data.PropInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/7/22 17:41
 */
public class SlotsUtil {
    private static final int TEN_THOUSAND = 10000;
    private static final BigDecimal TEN_THOUSAND_BIGDECIMAL = BigDecimal.valueOf(TEN_THOUSAND);

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

        if(prop == TEN_THOUSAND) {
            return all;
        }
        BigDecimal propValue = BigDecimal.valueOf(prop);
        BigDecimal divide = propValue.divide(TEN_THOUSAND_BIGDECIMAL, 4, BigDecimal.ROUND_HALF_UP);
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

        if(prop == TEN_THOUSAND) {
            return all;
        }
        BigDecimal propValue = BigDecimal.valueOf(prop);
        BigDecimal divide = propValue.divide(TEN_THOUSAND_BIGDECIMAL, 4, BigDecimal.ROUND_HALF_UP);
        BigDecimal multiply = BigDecimal.valueOf(all).multiply(divide);
        return multiply.intValue();
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

        if(prop == TEN_THOUSAND) {
            return true;
        }

        int rand = RandomUtils.randomMinMax(1,TEN_THOUSAND);
        return rand <= prop;
    }
}
