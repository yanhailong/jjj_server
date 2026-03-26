package com.jjg.game.ploy.utils;

import com.jjg.game.ploy.data.PropInfo;

import java.util.Map;

/**
 * @author 11
 * @date 2026/3/20
 */
public class PropUtils {
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
}
