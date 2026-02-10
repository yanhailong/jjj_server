package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.UserItem;
import com.jjg.game.core.base.condition.event.UserItemEvent;
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
public class BindPhoneCondition implements ConditionHandler<Integer> {
    private final AccountDao accountDao;

    public BindPhoneCondition(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public String type() {
        return "bindPhone";
    }

    @Override
    public Integer parse(List<String> args) {
        return 0;
    }

    public boolean matchCheck(UserItemEvent event, UserItem config) {
        return event.getItemId() == config.itemId();
    }

    @Override
    public MatchResultData match(ConditionContext ctx, Integer config) {
        Account account = accountDao.queryAccountByPlayerId(ctx.player().getId());
        if (account == null) {
            return MatchResultData.notMatch(Code.SUCCESS);
        }
        if (StringUtils.isNotEmpty(account.getThirdAccount(LoginType.PHONE))) {
            return MatchResultData.match();
        }
        return MatchResultData.notMatch(getErrorCode());
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
    public MatchResultData addProgress(ConditionContext ctx, Integer config) {
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
