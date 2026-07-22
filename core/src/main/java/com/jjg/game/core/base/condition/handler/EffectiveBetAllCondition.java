package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

/** 12001：累计指定游戏的有效押注，每达到目标值完成一次。 */
@Component
public class EffectiveBetAllCondition extends BaseEffectiveCondition {
    public EffectiveBetAllCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao, conditionRules, 12001);
    }

    @Override
    public String type() {
        return "effectiveBetAll";
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(12001);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
