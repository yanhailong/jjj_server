package com.jjg.game.hall.utils;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;

import java.util.Map;
import java.util.Objects;

/**
 * 检查是否满足条件
 *
 * @author lm
 * @date 2025/8/20 13:38
 */
public class ConditionUtil {
    public static int checkCondition(Player player, Map<Integer, Integer> condition) {
        if (CollectionUtil.isEmpty(condition)) {
            return Code.SUCCESS;
        }
        if (Objects.isNull(player)) {
            return Code.PARAM_ERROR;
        }
        for (Map.Entry<Integer, Integer> entry : condition.entrySet()) {
            Integer type = entry.getValue();
            switch (type) {
                //等级
                case 1 -> {
                    if (player.getLevel() < entry.getValue()) {
                        return Code.Level_NOT_ENOUGH;
                    }
                }
                //开服天数
                case 2 -> {
                    return Code.SUCCESS;
                }
                default -> {
                    return Code.PARAM_ERROR;
                }
            }
        }
        return Code.SUCCESS;
    }
}
