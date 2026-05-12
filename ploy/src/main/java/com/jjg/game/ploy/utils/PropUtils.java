package com.jjg.game.ploy.utils;

import com.jjg.game.ploy.data.PropInfo;

import java.util.Map;

/**
 * @author 11
 * @date 2026/3/20
 */
public class PropUtils {

    private static final int tenThousand = 10000;

    /**
     * 将 <值,权重>格式的map转化为PropInfo
     *
     * @param map
     * @return
     */
    public static PropInfo converMapToPropInfo(Map<Integer, Integer> map) {
        if (map == null || map.isEmpty()) {
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
