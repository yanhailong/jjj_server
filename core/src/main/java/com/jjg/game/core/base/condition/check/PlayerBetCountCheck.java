package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.check.record.PlayerSampleCondition;
import com.jjg.game.core.base.condition.check.record.PlayerSampleParam;

import java.util.List;

/**
 * 10001_游戏ID(0 = 任意游戏)（不区分倍场)_大于等于总押注条件_等于目标投注次数
 *
 * @author lm
 * @date 2025/10/16 17:52
 */
public class PlayerBetCountCheck extends BaseCheck {
    @Override
    public long addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerSampleParam param && conditionObject instanceof PlayerSampleCondition condition) {
            if (CollectionUtil.isEmpty(param.getParamList())) {
                return 0;
            }
            long progress = 0;
            if (CollectionUtil.isEmpty(condition.getIds()) || condition.getIds().contains(param.getId())) {
                progress = getProgress(param);
                int add = 0;
                for (Long betValue : param.getParamList()) {
                    if (betValue >= condition.getMinAchievedValue()) {
                        progress = progress + 1;
                        add++;
                    }
                }
                if (add > 0) {
                    countDao.incrBy(param.getFunction(), getCustomId(param.getPlayerId()), add);
                }
            }
            return (int) (progress / condition.getAchievedTimes());
        }
        return 0;
    }


    @Override
    public PlayerSampleCondition analysisCondition(List<Integer> condition) {
        if (condition.size() < 3) {
            return null;
        }
        PlayerSampleCondition playerSampleCondition = new PlayerSampleCondition();
        if (condition.getFirst() != 0) {
            playerSampleCondition.setIds(List.of(condition.getFirst()));
        }
        playerSampleCondition.setMinAchievedValue(condition.get(1));
        playerSampleCondition.setAchievedTimes(condition.get(2));
        return playerSampleCondition;
    }

    @Override
    public Achieved getAchievedType() {
        return Achieved.TIMES;
    }
}

