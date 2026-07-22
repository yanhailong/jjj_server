package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

/** 12002：非开房类游戏累计有效押注，每达到目标值完成一次。 */
@Component
public class EffectiveBetCondition extends BaseEffectiveCondition {
    public EffectiveBetCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao, conditionRules, 12002);
    }

    @Override
    public String type() {
        return "effectiveBet";
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(12002);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
