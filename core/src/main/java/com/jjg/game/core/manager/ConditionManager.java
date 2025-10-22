package com.jjg.game.core.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.EnumUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.base.condition.ConditionCheck;
import com.jjg.game.core.base.condition.ConditionType;
import com.jjg.game.core.data.Player;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lm
 * @date 2025/10/17 10:46
 */
@Component
public class ConditionManager {

    /**
     * 添加条件进度
     *
     * @param param     添加的条件参数
     * @param condition 条件
     * @return 添加后的进度
     */
    public BigDecimal addProgress(Object param, String condition) {
        if (StringUtils.isEmpty(condition) || param == null) {
            return BigDecimal.ZERO;
        }
        //解析条件
        List<String> conditionCfg = getConditionCfgList(condition);
        if (conditionCfg.size() < 2) {
            return BigDecimal.ZERO;
        }
        int id = Integer.parseInt(conditionCfg.removeFirst());
        ConditionType type = EnumUtil.getBy(ConditionType.class, (t) -> t.getId() == id);
        if (type == null) {
            return BigDecimal.ZERO;
        }
        return type.addProgress(param, conditionCfg);
    }

    /**
     * 添加进度并获取达成次数
     *
     * @param player    玩家
     * @param param     添加参数
     * @param condition 条件
     * @param reset     是否重置
     * @return 达成次数
     */
    public BigDecimal addProgressAndGetAchievements(Player player, Object param, String condition, boolean reset) {
        if (StringUtils.isEmpty(condition) || param == null || player == null) {
            return BigDecimal.ZERO;
        }
        List<String> conditionCfg = getConditionCfgList(condition);
        if (conditionCfg.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int id = Integer.parseInt(conditionCfg.removeFirst());
        ConditionType type = EnumUtil.getBy(ConditionType.class, (t) -> t.getId() == id);
        if (type == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = type.addProgress(param, conditionCfg);
        if (result.compareTo(BigDecimal.ZERO) > 0 && reset) {
            type.getConditionCheck().clearProgress(param);
        }
        return result;
    }

    /**
     * 获取解析后的条件限制列表
     *
     * @param condition 条件限制字符串
     * @return 解析后的条件限制列表
     */
    private List<String> getConditionCfgList(String condition) {
        return new ArrayList<>(Arrays.stream(StringUtils.split(condition, "_"))
                .toList());
    }

    /**
     * 是否达成条件
     *
     * @param player    玩家
     * @param param     添加参数
     * @param condition 条件参数
     * @return 是否达成条件
     */
    public boolean isAchievement(Player player, Object param, String condition) {
        if (StringUtils.isEmpty(condition) || player == null || param == null) {
            return false;
        }
        Pair<List<String>, ConditionType> conditionCfg = getConditionCfg(condition);
        if (conditionCfg == null) {
            return false;
        }
        ConditionType type = conditionCfg.getSecond();
        return type.doCheck(param, conditionCfg.getFirst());
    }

    /**
     * 是否达成条件
     *
     * @param player    玩家
     * @param param     添加参数
     * @param condition 条件参数
     * @return 是否达成条件
     */
    public boolean isAchievementByStr(Player player, Object param, List<String> condition) {
        if (CollectionUtil.isEmpty(condition) || player == null || param == null) {
            return false;
        }
        int id = Integer.parseInt(condition.removeFirst());
        ConditionType type = EnumUtil.getBy(ConditionType.class, (t) -> t.getId() == id);
        if (type == null) {
            return false;
        }
        return type.doCheck(param, condition);
    }

    /**
     * 是否达成条件
     *
     * @param player    玩家
     * @param param     添加参数
     * @param condition 条件参数
     * @return 是否达成条件
     */
    public boolean isAchievement(Player player, Object param, List<Integer> condition) {
        if (CollectionUtil.isEmpty(condition) || player == null || param == null) {
            return false;
        }
        int id = condition.removeFirst();
        ConditionType type = EnumUtil.getBy(ConditionType.class, (t) -> t.getId() == id);
        if (type == null) {
            return false;
        }
        return type.doCheck(param, condition.stream().map(String::valueOf).toList());
    }

    /**
     * 删除进度
     *
     * @param checkParam 添加参数
     * @param condition  条件参数
     * @param times      达成次数
     * @return 进度
     */
    public BigDecimal reduceProgress(Object checkParam, String condition, long times) {
        if (StringUtils.isEmpty(condition)) {
            return BigDecimal.ZERO;
        }
        Pair<List<String>, ConditionType> conditionCfg = getConditionCfg(condition);
        if (conditionCfg == null) {
            return BigDecimal.ZERO;
        }

        ConditionCheck check = conditionCfg.getSecond().getConditionCheck();
        Object object = check.analysisCondition(conditionCfg.getFirst());
        return check.reduceProgress(checkParam, object, times);
    }

    /**
     * 获取解析后的条件限制列表和条件类型
     *
     * @param condition 条件限制字符串
     * @return 解析后的条件限制列表和条件类型
     */
    private Pair<List<String>, ConditionType> getConditionCfg(String condition) {
        List<String> conditionCfg = getConditionCfgList(condition);
        if (conditionCfg.isEmpty()) {
            return null;
        }
        int id = Integer.parseInt(conditionCfg.removeFirst());
        ConditionType type = EnumUtil.getBy(ConditionType.class, (t) -> t.getId() == id);
        if (type == null) {
            return null;
        }
        return Pair.newPair(conditionCfg, type);
    }

    /**
     * 获取进度
     *
     * @param checkParam 添加参数
     * @param type       条件类型
     * @return 进度
     */
    public BigDecimal getProgress(Object checkParam, ConditionType type) {
        if (type == null) {
            return BigDecimal.ZERO;
        }
        return type.getProgress(checkParam);
    }
}
