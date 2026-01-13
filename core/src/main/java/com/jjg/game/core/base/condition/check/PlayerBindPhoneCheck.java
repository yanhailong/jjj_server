package com.jjg.game.core.base.condition.check;

import com.jjg.game.core.base.condition.ConditionCheck;
import com.jjg.game.core.data.Account;
import com.jjg.game.core.data.LoginType;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.List;

public class PlayerBindPhoneCheck implements ConditionCheck {
    @Override
    public boolean check(Object paramObject, Object conditionObject) {
        if(paramObject instanceof Account account){
            return StringUtils.isNotEmpty(account.getThirdAccount(LoginType.PHONE));
        }
        return false;
    }

    @Override
    public BigDecimal getProgress(Object param) {
        return BigDecimal.ZERO;
    }

    @Override
    public Object analysisCondition(List<String> condition) {
        return Integer.MAX_VALUE;
    }
}
