package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.check.record.PlayerEffectiveCondition;
import com.jjg.game.core.base.condition.check.record.PlayerEffectiveParam;
import com.jjg.game.core.base.condition.check.record.PlayerSampleCondition;

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
        if (paramObject instanceof PlayerEffectiveParam param && conditionObject instanceof PlayerEffectiveCondition condition) {
            if (CollectionUtil.isEmpty(param.getParamList())) {
                return 0;
            }
            long progress = 0;
            //开房间的类型大于十
            if (param.getRoomType() < 10) {
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
