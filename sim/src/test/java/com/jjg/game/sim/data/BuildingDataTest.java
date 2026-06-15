package com.jjg.game.sim.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildingDataTest {

    @Test
    void applySpeedupSecondsReducesUpgradeCd() {
        BuildingData data = new BuildingData();
        long now = 1_000_000L;
        data.setCdEndTime(now + 120_000L);

        long reducedSeconds = data.applySpeedupSeconds(45, now);

        assertEquals(45, reducedSeconds);
        assertEquals(now + 75_000L, data.getCdEndTime());
    }

    @Test
    void applySpeedupSecondsDoesNotMoveCdBeforeNow() {
        BuildingData data = new BuildingData();
        long now = 1_000_000L;
        data.setCdEndTime(now + 30_000L);

        long reducedSeconds = data.applySpeedupSeconds(90, now);

        assertEquals(30, reducedSeconds);
        assertEquals(now, data.getCdEndTime());
    }

    @Test
    void applySpeedupSecondsIgnoresNonUpgradingBuilding() {
        BuildingData data = new BuildingData();
        long now = 1_000_000L;
        data.setCdEndTime(now);

        long reducedSeconds = data.applySpeedupSeconds(30, now);

        assertEquals(0, reducedSeconds);
        assertEquals(now, data.getCdEndTime());
    }
}
