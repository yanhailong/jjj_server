package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.condition.numeric.StateConditionEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.data.Account;
import com.jjg.game.core.data.LoginType;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 4_1(需要绑定手机）
 *
 * @author lm
 * @date 2026/1/14 13:48
 */
@Component
public class BindPhoneCondition implements ConditionHandler<PreparedCondition> {
    private final AccountDao accountDao;
    private final ConditionRuleRegistry conditionRules;

    public BindPhoneCondition(AccountDao accountDao, ConditionRuleRegistry conditionRules) {
        this.accountDao = accountDao;
        this.conditionRules = conditionRules;
    }

    @Override
    public String type() {
        return "bindPhone";
    }

    @Override
    public EGameEventType eventType() {
        return EGameEventType.BIND_PHONE;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(4, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        Account account = accountDao.queryAccountByPlayerId(ctx.player().getId());
        if (account == null) {
            return MatchResultData.notMatch(Code.SUCCESS);
        }
        long bound = StringUtils.isNotEmpty(account.getThirdAccount(LoginType.PHONE)) ? 1 : 0;
        ConditionUpdate update = config.evaluate(
                new StateConditionEvent(StateConditionEvent.Type.PHONE_BOUND, 0, bound, 0));
        return update.completed(update.apply(0))
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), config.target(), bound);
    }

    public MatchResultData match(Long playerId) {
        Account account = accountDao.queryAccountByPlayerId(playerId);
        if (account == null) {
            return MatchResultData.notMatch(Code.SUCCESS);
        }
        if (StringUtils.isNotEmpty(account.getThirdAccount(LoginType.PHONE))) {
            return MatchResultData.match();
        }
        return MatchResultData.notMatch(getErrorCode());
    }
    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        return MatchResultData.unknown();
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(4);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }
}
