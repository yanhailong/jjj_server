package com.jjg.game.sim.service;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.data.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimTaskStateEventFactoryTest {
    private final ConditionRuleRegistry rules = ConditionRuleRegistry.standard();

    @Test
    void onlyStatesWithReliableSimDataSourcesAreAccepted() {
        assertTrue(SimTaskStateEventFactory.supports(condition("1_10")));
        assertFalse(SimTaskStateEventFactory.supports(condition("2_10")));
        assertTrue(SimTaskStateEventFactory.supports(condition("3_10")));
        assertFalse(SimTaskStateEventFactory.supports(condition("4_1")));
        assertFalse(SimTaskStateEventFactory.supports(condition("5_1_1_10")));
    }

    @Test
    void playerAndVipLevelsAreReadFromCurrentPlayerSnapshot() {
        Player player = new Player();
        player.setLevel(12);
        player.setVipLevel(4);

        assertEquals(12, SimTaskStateEventFactory.from(player, condition("1_10")).value());
        assertEquals(4, SimTaskStateEventFactory.from(player, condition("3_3")).value());
    }

    private PreparedCondition condition(String config) {
        return rules.prepare(ConditionSpec.parse(config));
    }
}
