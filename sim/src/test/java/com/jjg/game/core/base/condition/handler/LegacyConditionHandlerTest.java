package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 确保旧表达式入口只做数据源适配，参数格式统一由 numeric 注册表编译。 */
class LegacyConditionHandlerTest {
    private final ConditionRuleRegistry conditionRules = ConditionRuleRegistry.standard();

    @Test
    void everyLegacyHandlerCompilesTheConditionTableFormat() {
        assertPrepared(new PlayerLevelCondition(conditionRules), 1, s("2"), l(2));
        assertPrepared(new PlayerVipLevelCondition(conditionRules), 3, s("2"), l(2));
        assertPrepared(new BindPhoneCondition(null, conditionRules), 4, s("1"), l(1));
        assertPrepared(new RemainingAttemptsCondition(null, conditionRules), 5,
                s("77", "2", "5"), l(77, 2, 5));
        assertPrepared(new BetFrequencyCondition(null, conditionRules), 10001,
                s("0", "10", "3"), l(0, 10, 3));
        assertPrepared(new PlayGameCountCondition(null, conditionRules), 10002,
                s("0", "10", "3"), l(0, 10, 3));
        assertPrepared(new PlayGameWinMoneyCondition(conditionRules), 10003,
                s("0", "10", "100", "1"), l(0, 10, 100, 1));
        assertPrepared(new SingleRechargeCondition(null, conditionRules), 11001,
                s("100", "2", "0"), l(100, 2, 0));
        assertPrepared(new ActivityTotalRechargeCondition(null, conditionRules), 11002,
                s("0", "100"), l(0, 100));
        assertPrepared(new TodayDepositCondition(null, conditionRules), 11003,
                s("100", "0"), l(100, 0));
        assertPrepared(new TotalRechargeCondition(null, conditionRules), 11004,
                s("100", "0"), l(100, 0));
        assertPrepared(new AccountTotalRechargeCondition(null, conditionRules), 11005,
                s("100"), l(100));
        assertPrepared(new EffectiveBetAllCondition(null, conditionRules), 12001,
                s("0", "100"), l(0, 100));
        assertPrepared(new EffectiveBetCondition(null, conditionRules), 12002,
                s("100"), l(100));
        assertPrepared(new GameIdDorpCondition(null, conditionRules), 12003,
                s("100", "1001", "1002"), l(100, 1001, 1002));
        assertPrepared(new GameIdNotDorpCondition(null, conditionRules), 12004,
                s("100", "1001", "1002"), l(100, 1001, 1002));
        assertPrepared(new GameTypeDropCondition(null, conditionRules), 12005,
                s("100", "1", "2"), l(100, 1, 2));
        assertPrepared(new GameRoomTypeDropCondition(null, conditionRules), 12006,
                s("100", "1", "2"), l(100, 1, 2));
        assertPrepared(new TotalValidBetsCondition(null, conditionRules), 12007,
                s("100"), l(100));
        assertPrepared(new UseItemCountCondition(null, conditionRules), 12101,
                s("2001", "3"), l(2001, 3));
    }

    private static void assertPrepared(ConditionHandler<PreparedCondition> handler, int id,
                                       List<String> args, List<Long> parameters) {
        PreparedCondition condition = handler.parse(args);
        assertEquals(id, condition.spec().id());
        assertEquals(parameters, condition.spec().parameters());
    }

    private static List<String> s(String... values) {
        return List.of(values);
    }

    private static List<Long> l(long... values) {
        return java.util.Arrays.stream(values).boxed().toList();
    }
}
