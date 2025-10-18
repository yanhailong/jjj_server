package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.check.record.PlayerSampleCondition;
import com.jjg.game.core.base.condition.check.record.PlayerSampleParam;

import java.util.List;

/**
 * 12102_道具ID_等于大于目标数量(游戏下注产生的消耗不计算，其它地方都计算 例 系统升级、兑换 )
 *
 * @author lm
 * @date 2025/10/16 17:52
 */
public class PlayUseItemCheck extends BaseCheck {

    @Override
    public long addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerSampleParam param && conditionObject instanceof PlayerSampleCondition condition) {

            if (CollectionUtil.isEmpty(param.getParamList())) {
                return 0;
            }
            long progress = countDao.incrBy(param.getFunction(), getCustomId(param.getPlayerId()), param.getParamList().getFirst());
            return progress >= condition.getAchievedTimes() ? 1 : 0;
        }
        return 0;
    }

    @Override
    public PlayerSampleCondition analysisCondition(List<Integer> condition) {
        if (condition.size() < 2) {
            return null;
        }
        PlayerSampleCondition sampleCondition = new PlayerSampleCondition();
        sampleCondition.setItemId(condition.getFirst());
        sampleCondition.setAchievedTimes(condition.get(1));
        return sampleCondition;
    }


    @Override
    public Achieved getAchievedType() {
        return Achieved.TIMES;
    }
}

