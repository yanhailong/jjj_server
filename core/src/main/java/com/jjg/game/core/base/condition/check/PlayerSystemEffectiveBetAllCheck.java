package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.check.record.PlayerEffectiveCondition;
import com.jjg.game.core.base.condition.check.record.PlayerEffectiveParam;
import com.jjg.game.core.base.condition.check.record.PlayerSampleCondition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 12002_每累积达到设定有效下注数量（不计算开房间游戏）
 *
 * @author lm
 * @date 2025/10/17 10:11
 */
public class PlayerSystemEffectiveBetAllCheck extends BaseCheck {
    @Override
    public BigDecimal addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerEffectiveParam param && conditionObject instanceof PlayerEffectiveCondition condition) {
            if (CollectionUtil.isEmpty(param.getParamList())) {
                return BigDecimal.ZERO;
            }
            long progress = 0;
            //开房间的类型大于十
            if (param.getRoomType() < 10) {
                progress = countDao.incrBy(param.getFunction(), getCustomId(param.getPlayerId()), BigDecimal.valueOf(param.getParamList().getFirst())).longValue();
            }
            return progress >= condition.getMinAchievedValue().longValue() ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    @Override
    public PlayerSampleCondition analysisCondition(List<String> condition) {
        if (condition.isEmpty()) {
            return null;
        }
        PlayerSampleCondition sampleCondition = new PlayerSampleCondition();
        sampleCondition.setMinAchievedValue(new BigDecimal(condition.getFirst()).setScale(2, RoundingMode.DOWN));
        return sampleCondition;
    }

    @Override
    public Achieved getAchievedType() {
        return Achieved.PROGRESS;
    }
}
