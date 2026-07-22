package com.jjg.game.core.task.condition;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.task.db.TaskDetail;
import com.jjg.game.core.task.param.TaskConditionParam10001;
import com.jjg.game.core.task.param.TaskConditionParam10003;
import com.jjg.game.core.task.param.TaskConditionParam12001;
import com.jjg.game.core.task.param.TaskConditionParam12101;
import com.jjg.game.core.task.param.TaskConditionParamRecharge;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskConditionAdapterTest {
    private final ConditionRuleRegistry conditionRules = ConditionRuleRegistry.standard();

    @Test
    void condition10003UsesActualTotalBetThreshold() {
        TaskCondition10003 handler = new TaskCondition10003(conditionRules);
        TestTaskCfg cfg = new TestTaskCfg(9001, List.of(10003L, 1001L, 100L, 50L, 1L));
        TaskDetail detail = new TaskDetail();

        assertFalse(handler.trigger(7, cfg, detail, event(1001, 99, 60, 1)));
        assertTrue(handler.trigger(7, cfg, detail, event(1001, 100, 60, 1)));
    }

    @Test
    void taskConditionCacheReusesCompiledRuleAndRefreshesChangedConfig() {
        TaskConditionCache cache = new TaskConditionCache(conditionRules);
        TestTaskCfg cfg = new TestTaskCfg(9002, List.of(11002L, 0L, 100L));

        PreparedCondition first = cache.get(cfg);
        assertSame(first, cache.get(cfg));

        cfg.condition = List.of(11002L, 0L, 200L);
        PreparedCondition changed = cache.get(cfg);
        assertTrue(first != changed);
        assertSame(changed, cache.get(cfg));
    }

    @Test
    void legacyTaskAdaptersUseRegistryFiltersAndTargets() {
        TaskCondition10001 betCount = new TaskCondition10001(conditionRules);
        TestTaskCfg betCfg = new TestTaskCfg(9003, List.of(10001L, 1001L, 100L, 2L));
        TaskDetail betDetail = new TaskDetail();
        assertFalse(betCount.trigger(7, betCfg, betDetail, betEvent(1002, 100)));
        assertFalse(betCount.trigger(7, betCfg, betDetail, betEvent(1001, 99)));
        assertTrue(betCount.trigger(7, betCfg, betDetail, betEvent(1001, 100)));
        assertEquals(1L, betDetail.getProgress().get(10001));

        TaskCondition12001 effectiveBet = new TaskCondition12001(conditionRules);
        TestTaskCfg effectiveCfg = new TestTaskCfg(9004, List.of(12001L, 1001L, 200L));
        TaskDetail effectiveDetail = new TaskDetail();
        assertFalse(effectiveBet.trigger(7, effectiveCfg, effectiveDetail, effectiveBetEvent(1002, 200)));
        assertTrue(effectiveBet.trigger(7, effectiveCfg, effectiveDetail, effectiveBetEvent(1001, 200)));
        assertEquals(200L, effectiveDetail.getProgress().get(12001));

        TaskCondition12101 itemUse = new TaskCondition12101(conditionRules);
        TestTaskCfg itemCfg = new TestTaskCfg(9005, List.of(12101L, 10001L, 3L));
        TaskDetail itemDetail = new TaskDetail();
        assertFalse(itemUse.trigger(7, itemCfg, itemDetail, itemEvent(10002, 3)));
        assertTrue(itemUse.trigger(7, itemCfg, itemDetail, itemEvent(10001, 3)));
        assertEquals(3L, itemDetail.getProgress().get(12101));
    }

    @Test
    void rechargeTaskAdaptersUsePaymentChannel() {
        TaskCondition11001 singleRecharge = new TaskCondition11001(conditionRules);
        TestTaskCfg singleCfg = new TestTaskCfg(9006, List.of(11001L, 100L, 2L, 7L));
        TaskDetail singleDetail = new TaskDetail();
        assertFalse(singleRecharge.trigger(7, singleCfg, singleDetail, rechargeEvent(8, 100)));
        assertFalse(singleRecharge.trigger(7, singleCfg, singleDetail, rechargeEvent(7, 99)));
        assertTrue(singleRecharge.trigger(7, singleCfg, singleDetail, rechargeEvent(7, 100)));
        assertEquals(1L, singleDetail.getProgress().get(11001));

        TaskCondition11002 totalRecharge = new TaskCondition11002(conditionRules);
        TestTaskCfg totalCfg = new TestTaskCfg(9007, List.of(11002L, 7L, 300L));
        TaskDetail totalDetail = new TaskDetail();
        assertFalse(totalRecharge.trigger(7, totalCfg, totalDetail, rechargeEvent(8, 300)));
        assertTrue(totalRecharge.trigger(7, totalCfg, totalDetail, rechargeEvent(7, 300)));
        assertEquals(300L, totalDetail.getProgress().get(11002));
    }

    private static TaskConditionParam10003 event(int gameId, long bet, long win, int itemId) {
        TaskConditionParam10003 param = new TaskConditionParam10003();
        param.setGameId(gameId);
        param.setBetAmount(bet);
        param.setAddValue(win);
        param.setCoinId(itemId);
        return param;
    }

    private static TaskConditionParam10001 betEvent(int gameId, long bet) {
        TaskConditionParam10001 param = new TaskConditionParam10001();
        param.setGameId(gameId);
        param.setAddValue(bet);
        return param;
    }

    private static TaskConditionParam12001 effectiveBetEvent(int gameId, long bet) {
        TaskConditionParam12001 param = new TaskConditionParam12001();
        param.setGameId(gameId);
        param.setAddValue(bet);
        return param;
    }

    private static TaskConditionParam12101 itemEvent(int itemId, long count) {
        TaskConditionParam12101 param = new TaskConditionParam12101();
        param.setItemId(itemId);
        param.setAddValue(count);
        return param;
    }

    private static TaskConditionParamRecharge rechargeEvent(int channelId, long amount) {
        TaskConditionParamRecharge param = new TaskConditionParamRecharge();
        param.setChannelId(channelId);
        param.setAddValue(amount);
        return param;
    }

    private static final class TestTaskCfg extends TaskCfg {
        private List<Long> condition;

        private TestTaskCfg(int id, List<Long> condition) {
            this.id = id;
            this.condition = condition;
        }

        @Override
        public List<Long> getTaskConditionId() {
            return condition;
        }
    }
}
