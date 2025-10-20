package com.jjg.game.core.base.condition.check;

import com.jjg.game.core.base.condition.ConditionCheck;
import com.jjg.game.core.base.condition.check.record.BaseCheckCondition;
import com.jjg.game.core.base.condition.check.record.BaseCheckParam;
import com.jjg.game.core.dao.CountDao;

/**
 * 基础检查基类
 * @author lm
 * @date 2025/10/16 18:26
 */
public abstract class BaseCheck implements ConditionCheck {
    /**
     * 计数dao
     */
    protected CountDao countDao;

    public CountDao getCountDao() {
        return countDao;
    }

    public void setCountDao(CountDao countDao) {
        this.countDao = countDao;
    }

    public String getCustomId(long playerId) {
        return getClass().getSimpleName() + playerId;
    }

    @Override
    public void clearProgress(Object checkParam) {
        if (checkParam instanceof BaseCheckParam param) {
            countDao.reset(param.getFunction(), getCustomId(param.getPlayerId()));
        }
    }

    @Override
    public long getProgress(Object paramObject) {
        if (paramObject instanceof BaseCheckParam param) {
            return countDao.getCount(param.getFunction(), getCustomId(param.getPlayerId()));
        }
        return 0;
    }

    @Override
    public boolean check(Object paramObject, Object conditionObject) {
        if (paramObject instanceof BaseCheckParam param && conditionObject instanceof BaseCheckCondition condition) {
            return switch (getAchievedType()) {
                case TIMES -> getProgress(param) >= condition.getAchievedTimes();
                case PROGRESS -> getProgress(param) >= condition.getMinAchievedValue();
            };
        }
        return false;
    }

    public abstract Achieved getAchievedType();

    public enum Achieved {
        //次数
        TIMES,
        //进度
        PROGRESS
    }
}
