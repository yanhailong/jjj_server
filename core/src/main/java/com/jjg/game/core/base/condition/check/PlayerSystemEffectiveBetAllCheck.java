package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.check.record.PlayerSampleCondition;
import com.jjg.game.core.base.condition.check.record.PlayerSampleParam;

import java.util.List;

/**
 * 12002_每累积达到设定有效下注数量（不计算开房间游戏）
 *
 * @author lm
 * @date 2025/10/17 10:11
 */
public class PlayerSystemEffectiveBetAllCheck extends BaseCheck {
    @Override
    public long addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerSampleParam param && conditionObject instanceof PlayerSampleCondition condition) {
            if (CollectionUtil.isEmpty(param.getParamList())) {
                return 0;
            }
            long progress = 0;
            if (param.getId() < 0) {
                progress = countDao.incrBy(param.getFunction(), getCustomId(param.getPlayerId()), param.getParamList().getFirst());
            }
            return progress >= condition.getMinAchievedValue() ? 1 : 0;
        }
        return 0;
    }

    @Override
    public PlayerSampleCondition analysisCondition(List<Integer> condition) {
        if (condition.isEmpty()) {
            return null;
        }
        PlayerSampleCondition sampleCondition = new PlayerSampleCondition();
        sampleCondition.setMinAchievedValue(condition.getFirst());
        return sampleCondition;
    }

    @Override
    public Achieved getAchievedType() {
        return Achieved.PROGRESS;
    }
}
