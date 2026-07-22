package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

/** 12005：仅统计指定游戏类型的有效押注。 */
@Component
public class GameTypeDropCondition extends BaseEffectiveCondition {
    public GameTypeDropCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao, conditionRules, 12005);
    }

    @Override
    public String type() {
        return "gameTypeDrop";
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(12005);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
