package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

/** 12004：排除指定游戏范围后统计有效押注。 */
@Component
public class GameIdNotDorpCondition extends BaseEffectiveCondition {
    public GameIdNotDorpCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao, conditionRules, 12004);
    }

    @Override
    public String type() {
        return "gameIdNotDorp";
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(12004);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
