package com.jjg.game.gm.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author 11
 * @date 2025/9/19 10:15
 */
public record ShopProductDto(
        //产品id
        int id,
        //类型
        int type,
        //条件
        Map<Integer,Integer> conditionsMap,
        //是否开启
        boolean open,
        //开启时间
        int startTime,
        //关闭时间
        int endTime,
        //奖励道具
        Map<Integer,Long> rewardItems,
        //价值类型
        int valueType,
        //价值
        long value,
        //购买类型  -1.充值  ,其他值则为道具id
        int payType,
        //价格
        BigDecimal money,
        //标签1
        int label1,
        //标签2
        int label2,
        //图片
        String pic
) {
}
