package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

/** 12003：仅统计指定游戏范围的有效押注。 */
@Component
public class GameIdDorpCondition extends BaseEffectiveCondition {
    public GameIdDorpCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao, conditionRules, 12003);
    }

    @Override
    public String type() {
        return "gameIdDorp";
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(12003);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
