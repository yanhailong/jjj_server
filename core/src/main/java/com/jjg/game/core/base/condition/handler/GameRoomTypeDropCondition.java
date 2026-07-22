package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

/** 12006：仅统计指定房间类型的有效押注。 */
@Component
public class GameRoomTypeDropCondition extends BaseEffectiveCondition {
    public GameRoomTypeDropCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao, conditionRules, 12006);
    }

    @Override
    public String type() {
        return "gameRoomTypeDrop";
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(12006);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
