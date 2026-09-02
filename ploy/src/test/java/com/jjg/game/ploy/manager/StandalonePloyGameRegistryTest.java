package com.jjg.game.ploy.manager;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StandalonePloyGameRegistryTest {

    @Test
    void resolvesRegisteredStandaloneGame() {
        StandalonePloyGame mining = () -> 400800;
        StandalonePloyGameRegistry registry = new StandalonePloyGameRegistry(List.of(mining));

        assertSame(mining, registry.get(400800));
        assertNull(registry.get(400700));
    }

    @Test
    void rejectsDuplicateGameType() {
        assertThrows(IllegalStateException.class, () -> new StandalonePloyGameRegistry(List.of(
                () -> 400800,
                () -> 400800)));
    }
}
