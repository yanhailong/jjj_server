package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.check.record.PlayerSampleCondition;
import com.jjg.game.core.base.condition.check.record.PlayerSampleParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 10002_游戏ID(0 = 任意游戏)（不区分倍场）_大于等于总押注条件_等于目标局数次数
 *
 * @author lm
 * @date 2025/10/16 17:52
 */
public class PlayerGameCountCheck extends BaseCheck {

    @Override
    public BigDecimal addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerSampleParam param && conditionObject instanceof PlayerSampleCondition condition) {
            if (CollectionUtil.isEmpty(param.getParamList())) {
                return BigDecimal.ZERO;
            }
            long progress = 0;
            if (CollectionUtil.isEmpty(condition.getIds()) || condition.getIds().contains(param.getId())) {
                progress = getProgress(param).longValue();
                Long first = param.getParamList().getFirst();
                if (first >= condition.getMinAchievedValue().longValue()) {
                    progress = progress + 1;
                    countDao.incr(param.getFunction(), getCustomId(param.getPlayerId()));
                }

            }
            return BigDecimal.valueOf(progress / condition.getAchievedTimes());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public PlayerSampleCondition analysisCondition(List<String> condition) {
        if (condition.size() < 3) {
            return null;
        }
        PlayerSampleCondition sampleCondition = new PlayerSampleCondition();
        int id = Integer.parseInt(condition.getFirst());
        if (id > 0) {
            sampleCondition.setIds(List.of(id));
        }
        sampleCondition.setMinAchievedValue(new BigDecimal(condition.get(1)).setScale(2, RoundingMode.DOWN));
        sampleCondition.setAchievedTimes(Integer.parseInt(condition.get(2)));
        return sampleCondition;
    }

    @Override
    public Achieved getAchievedType() {
        return Achieved.TIMES;
    }
}

