package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.check.record.PlayerEffectiveDropCondition;
import com.jjg.game.core.base.condition.check.record.PlayerEffectiveDropParam;

import java.util.List;

/**
 * 12001_游戏ID(0 = 任意游戏)_每累积达到设定有效下注数量（计算所有游戏）
 *
 * @author lm
 * @date 2025/10/17 10:11
 */
public class PlayerEffectiveBetAllCheck extends BaseCheck {

    @Override
    public long addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerEffectiveDropParam param && conditionObject instanceof PlayerEffectiveDropCondition condition) {
            if (CollectionUtil.isEmpty(param.getParamList())) {
                return 0;
            }
            long progress = 0;
            if (CollectionUtil.isEmpty(condition.getGameIds()) || condition.getGameIds().contains(param.getGameId())) {
                progress = countDao.incrBy(param.getFunction(), getCustomId(param.getPlayerId()), param.getParamList().getFirst());
            }
            return (int) (progress / condition.getMinAchievedValue());
        }
        return 0;
    }

    @Override
    public PlayerEffectiveDropCondition analysisCondition(List<Integer> condition) {
        if (condition.size() < 2) {
            return null;
        }
        PlayerEffectiveDropCondition sampleCondition = new PlayerEffectiveDropCondition();
        if (condition.getFirst() > 0) {
            sampleCondition.setRoomTypeIds(List.of(condition.getFirst()));
        }
        sampleCondition.setMinAchievedValue(condition.get(1));
        return sampleCondition;
    }


    @Override
    public Achieved getAchievedType() {
        return Achieved.PROGRESS;
    }
}
