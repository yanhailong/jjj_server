package com.jjg.game.core.base.condition.numeric;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResult;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.base.condition.handler.ActivityTotalRechargeCondition;
import com.jjg.game.core.base.condition.handler.PlayGameWinMoneyCondition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionRuleRegistryTest {
    private final ConditionRuleRegistry conditionRules = ConditionRuleRegistry.standard();

    @Test
    void unifiedRegistryCoversEveryConditionRemainingInSheet() {
        List<List<Long>> configs = List.of(
                c(1, 1), c(2, 1), c(3, 1), c(4, 1), c(5, 1, 1, 1),
                c(10001, 0, 1, 1), c(10002, 0, 1, 1), c(10003, 0, 1, 1, 1),
                c(11001, 1, 1, 0), c(11002, 0, 1), c(11003, 1, 0), c(11004, 1, 0), c(11005, 1),
                c(12001, 0, 1), c(12002, 1), c(12003, 1, 100100), c(12004, 1, 100100),
                c(12005, 1, 1), c(12006, 1, 1), c(12007, 1), c(12101, 1, 1),
                c(12201, 0, 1), c(12202, 0, 1, 1), c(12203, 1, 1), c(12204, 1, 1),
                c(12205, 1), c(12206, 0, 1), c(12207, 0, 1), c(12208, 1, 1), c(12209, 1),
                c(12210, 1, 1), c(12211, 0, 1), c(12212, 0, 1, 1), c(12213, 0, 1),
                c(12214, 1, 1), c(12215, 1, 1), c(12216, 1), c(12217, 1), c(12218, 1),
                c(12219, 1), c(12220, 1, 1),
                c(12301, 0, 1, 1, 1), c(12302, 0, 1, 1), c(12303, 0, 1), c(12304, 0, 1),
                c(12305, 1, 1), c(12306, 0, 1, 1, 1), c(12307, 0, 1),
                c(12501, 0, 1, 1, 1), c(12502, 0, 1, 1), c(12503, 0, 1, 1, 1), c(12504, 0, 1, 1),
                c(12601, 0, 1, 1), c(12602, 0, 1, 1, 1), c(12603, 0, 1, 1, 1),
                c(12604, 0, 1, 1), c(12605, 0, 1, 1, 1), c(12606, 0, 1, 1),
                c(12607, 0, 1), c(12608, 0, 1), c(12609, 0, 1, 1, 1), c(12610, 0, 1, 1, 1),
                c(12701, 0, 1), c(12702, 0, 1), c(12703, 0, 1, 1), c(12704, 0, 1, 1));

        assertEquals(66, configs.size());
        configs.forEach(config -> assertTrue(conditionRules.prepare(ConditionSpec.from(config)).target() > 0));
        for (int removed = 12401; removed <= 12408; removed++) {
            assertFalse(conditionRules.supports(removed));
        }
        assertSame(conditionRules, ConditionRuleRegistry.standard());
    }

    @Test
    void clarifiedConditionFormatsAndSemanticsAreEnforced() {
        PreparedCondition winMoney = conditionRules.prepare(ConditionSpec.parse("10003_100100*10*100*1"));
        assertEquals(120, winMoney.evaluate(game(100100, 10, 120, 1)).apply(0));
        assertEquals(0, winMoney.evaluate(game(100100, 9, 120, 1)).apply(0));
        assertThrows(IllegalArgumentException.class,
                () -> conditionRules.prepare(ConditionSpec.parse("10003_100100*10*1*100*1")));

        PreparedCondition recharge = conditionRules.prepare(ConditionSpec.parse("11002_7_500"));
        assertFalse(recharge.evaluate(new RechargeConditionEvent(8, 500)).matched());
        assertEquals(300, recharge.evaluate(new RechargeConditionEvent(7, 300)).apply(0));
        assertEquals(500, recharge.target());

        PreparedCondition historicalRecharge = conditionRules.prepare(ConditionSpec.parse("11002_500_0"));
        assertEquals(List.of(0L, 500L), historicalRecharge.spec().parameters());
        assertEquals(500, historicalRecharge.target());

        PreparedCondition singleWin = conditionRules.prepare(ConditionSpec.parse("12201_100100_100"));
        long progress = singleWin.evaluate(game(100100, 1, 120, 1)).apply(0);
        progress = singleWin.evaluate(game(100100, 1, 90, 1)).apply(progress);
        assertEquals(120, progress);
    }

    @Test
    void legacyExpressionHandlerDelegates10003ToNumericRule() {
        PlayGameWinMoneyCondition handler = new PlayGameWinMoneyCondition(conditionRules);
        PreparedCondition condition = handler.parse(List.of("100100", "10", "100", "1"));
        BetEvent event = new BetEvent();
        event.setGameId(100100);
        event.setGameType(100100);
        event.setItemId(1);
        event.setBetAmount(10);
        event.setWinAmount(120);

        assertEquals(MatchResult.MATCH,
                handler.match(new ConditionContext(null, event, ""), condition).result());
    }

    @Test
    void legacyRechargeHandlerUsesCentral11002Normalization() {
        ActivityTotalRechargeCondition handler = new ActivityTotalRechargeCondition(null, conditionRules);

        PreparedCondition condition = handler.parse(List.of("500", "0"));

        assertEquals(List.of(0L, 500L), condition.spec().parameters());
        assertEquals(500, condition.target());
    }

    private static List<Long> c(long... values) {
        return java.util.Arrays.stream(values).boxed().toList();
    }

    private static GameConditionEvent game(int gameId, long bet, long win, int itemId) {
        return new GameConditionEvent(gameId, gameId, 0, itemId, itemId,
                bet, win, 0, true, true, 0, 0, 0,
                Set.of(), List.of(), Map.of(itemId, win));
    }
}
