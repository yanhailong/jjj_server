package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.check.record.PlayerSampleCondition;
import com.jjg.game.core.base.condition.check.record.PlayerSampleParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 10001_游戏ID(0 = 任意游戏)（不区分倍场)_大于等于总押注条件_等于目标投注次数
 *
 * @author lm
 * @date 2025/10/16 17:52
 */
public class PlayerBetCountCheck extends BaseCheck {
    @Override
    public BigDecimal addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerSampleParam param && conditionObject instanceof PlayerSampleCondition condition) {
            if (CollectionUtil.isEmpty(param.getParamList())) {
                return BigDecimal.ZERO;
            }
            long progress = 0;
            if (CollectionUtil.isEmpty(condition.getIds()) || condition.getIds().contains(param.getId())) {
                progress = getProgress(param).longValue();
                int add = 0;
                for (Long betValue : param.getParamList()) {
                    if (betValue >= condition.getMinAchievedValue().longValue()) {
                        progress = progress + 1;
                        add++;
                    }
                }
                if (add > 0) {
                    countDao.incrBy(param.getFunction(), getCustomId(param.getPlayerId()), BigDecimal.valueOf(add));
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
        PlayerSampleCondition playerSampleCondition = new PlayerSampleCondition();
        int gameId = Integer.parseInt(condition.getFirst());
        if (gameId != 0) {
            playerSampleCondition.setIds(List.of(gameId));
        }
        playerSampleCondition.setMinAchievedValue(new BigDecimal(condition.get(1)).setScale(2, RoundingMode.DOWN));
        playerSampleCondition.setAchievedTimes(Integer.parseInt(condition.get(2)));
        return playerSampleCondition;
    }

    @Override
    public Achieved getAchievedType() {
        return Achieved.TIMES;
    }
}

