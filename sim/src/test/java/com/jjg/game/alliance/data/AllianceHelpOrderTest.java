package com.jjg.game.alliance.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllianceHelpOrderTest {

    @Test
    void helpedCountUsesPersistedCounterWhenPresent() {
        AllianceHelpOrder order = new AllianceHelpOrder();
        order.setHelpCount(3);
        order.setHelpers(Map.of(1001L, 1L));

        assertEquals(3, order.helpedCount());
    }

    @Test
    void fullUsesPersistedHelpCount() {
        AllianceHelpOrder order = new AllianceHelpOrder();
        order.setMaxHelp(2);
        order.setHelpCount(2);

        assertTrue(order.full());
    }
}
