package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.check.record.PlayerEffectiveCondition;
import com.jjg.game.core.base.condition.check.record.PlayerEffectiveParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 12001_游戏ID(0 = 任意游戏)_每累积达到设定有效下注数量（计算所有游戏）
 *
 * @author lm
 * @date 2025/10/17 10:11
 */
public class PlayerEffectiveBetAllCheck extends BaseCheck {

    @Override
    public BigDecimal addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerEffectiveParam param && conditionObject instanceof PlayerEffectiveCondition condition) {
            if (CollectionUtil.isEmpty(param.getParamList())) {
                return BigDecimal.ZERO;
            }
            BigDecimal progress =BigDecimal.ZERO;
            if (CollectionUtil.isEmpty(condition.getGameIds()) || condition.getGameIds().contains(param.getGameId())) {
                progress = countDao.incrBy(param.getFunction(), getCustomId(param.getPlayerId()), BigDecimal.valueOf(param.getParamList().getFirst()));
            }
            return progress.divide(condition.getMinAchievedValue(), RoundingMode.DOWN);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public PlayerEffectiveCondition analysisCondition(List<String> condition) {
        if (condition.size() < 2) {
            return null;
        }
        PlayerEffectiveCondition sampleCondition = new PlayerEffectiveCondition();
        int roomTypeId = Integer.parseInt(condition.getFirst());
        if (roomTypeId > 0) {
            sampleCondition.setRoomTypeIds(List.of(roomTypeId));
        }
        sampleCondition.setMinAchievedValue(new BigDecimal(condition.get(1)).setScale(2, RoundingMode.DOWN));
        return sampleCondition;
    }


    @Override
    public Achieved getAchievedType() {
        return Achieved.PROGRESS;
    }
}
