package com.jjg.game.ploy.games.mining;

import com.alibaba.fastjson.JSON;
import com.jjg.game.sampledata.GameDataManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MiningEngineTest {
    @BeforeAll static void loadTables() throws Exception { MiningFixtures.install(); }

    @Test void actualMapTableLoadsAllTriplesWithoutCollapsingRepeatedTypeIds() {
        var stage = GameDataManager.getMiningMapGenerationCfg(10001);
        assertEquals(8, stage.getFixedGrid().size());
        assertEquals(6, stage.getFixedGrid().stream().filter(e -> e.getFirst() == 1011).count());
        assertEquals(List.of(1001, 90, 50), stage.getRandomizedgrid().getFirst());
        assertEquals(6, stage.getWidth());
        assertEquals(24, GameDataManager.getMiningMapGenerationCfgList().size());
    }

    @Test void pickRequiresOrthogonalAdjacencyAndCountsOnlyDestroyedCells() {
        MiningEngine engine = new MiningEngine();
        MiningState state = MiningFixtures.flat(2, 1002);
        assertEquals("CELL_NOT_CONNECTED", assertThrows(MiningException.class, () -> engine.dig(state, 2, 2, 101, 1)).getMessage());
        engine.dig(state, 1, 1, 101, 1);
        assertEquals(1, MiningFixtures.cell(state, 1, 1).hp); assertEquals(0, state.total.grids);
        assertFalse(engine.connected(state, 2, 1));
        engine.dig(state, 1, 1, 101, 2);
        assertTrue(engine.connected(state, 2, 1)); assertFalse(engine.connected(state, 2, 2));
        assertEquals(1, state.total.grids); assertEquals(2, state.total.tools.get(1024034));
    }

    @Test void bombAffectsOnlyRowAndDoesNotDamageGranite() {
        MiningEngine engine = new MiningEngine();
        MiningState state = MiningFixtures.flat(3, 1003);
        MiningFixtures.cell(state, 4, 2).type = 1011; MiningFixtures.cell(state, 4, 2).hp = 9000;
        var result = engine.dig(state, 4, 4, 102, 100);
        assertEquals(5, result.changed().size()); assertEquals(5, state.total.grids);
        assertEquals(9000, MiningFixtures.cell(state, 4, 2).hp);
        assertEquals(3, MiningFixtures.cell(state, 3, 4).hp);
        assertEquals(3, MiningFixtures.cell(state, 5, 4).hp);
        assertEquals(4, state.depth);
    }

    @Test void excavatorCropsAtVisibleEdgesAndClearsGranite() {
        MiningEngine engine = new MiningEngine();
        MiningState state = MiningFixtures.flat(9000, 1011);
        assertEquals("WALL_REQUIRES_EXCAVATOR", assertThrows(MiningException.class, () -> engine.dig(state, 1, 1, 101, 1)).getMessage());
        assertThrows(MiningException.class, () -> engine.dig(state, 1, 1, 102, 1));
        assertEquals(4, engine.dig(state, 1, 1, 103, 1).changed().size());
        assertEquals(4, state.total.grids); assertEquals(9000, MiningFixtures.cell(state, 3, 1).hp);
    }

    @Test void connectedOnlyDescribesReachabilityAndIncludesGranite() {
        MiningEngine engine = new MiningEngine();
        MiningState state = MiningFixtures.flat(9000, 1011);
        MiningFixtures.cell(state, 1, 1).hp = 0;
        MiningFixtures.cell(state, 1, 1).reachable = true;

        assertTrue(MiningService.cellInfo(MiningFixtures.cell(state, 2, 1), state, engine).connected);
        assertFalse(MiningService.cellInfo(MiningFixtures.cell(state, 2, 2), state, engine).connected);
    }

    @Test void isolatedOpenAreaDoesNotMakeAdjacentCellsConnected() {
        MiningEngine engine = new MiningEngine();
        MiningState state = MiningFixtures.flat(1, 1001);
        MiningFixtures.cell(state, 1, 1).hp = 0;
        MiningFixtures.cell(state, 1, 1).reachable = true;

        engine.dig(state, 5, 5, 103, 1);

        assertTrue(engine.connected(state, 2, 1));
        assertFalse(engine.connected(state, 7, 5));
        assertTrue(state.cells.stream().filter(cell -> cell.row >= 4 && cell.row <= 6
                && cell.column >= 4 && cell.column <= 6).noneMatch(cell -> cell.reachable));
    }

    @Test void oldFirstScreenStateRestoresOnlySurfaceConnectedOpenArea() {
        MiningEngine engine = new MiningEngine();
        MiningState state = MiningFixtures.flat(1, 1001);
        state.connectivityVersion = 0;
        MiningFixtures.cell(state, 1, 1).hp = 0;
        MiningFixtures.cell(state, 2, 1).hp = 0;
        MiningFixtures.cell(state, 7, 3).hp = 0;

        assertTrue(engine.alignConnectivity(state));
        assertTrue(MiningFixtures.cell(state, 2, 1).reachable);
        assertFalse(MiningFixtures.cell(state, 7, 3).reachable);
        assertFalse(engine.connected(state, 7, 4));
        assertFalse(engine.alignConnectivity(state));
    }

    @Test void strandedVersionOneStateRestoresReachabilityFromOpenedTopRow() {
        MiningEngine engine = new MiningEngine();
        MiningState state = MiningFixtures.flat(1, 1001);
        state.topRow = 324;
        state.generatedRows = 331;
        state.connectivityVersion = 1;
        state.cells.clear();
        for (int row = 324; row <= 331; row++) {
            for (int column = 1; column <= 6; column++) {
                state.cells.add(new MiningState.Cell(row, column, 1001, 1));
            }
        }
        for (int row = 324; row <= 330; row++) MiningFixtures.cell(state, row, 3).hp = 0;

        assertTrue(engine.alignConnectivity(state));
        assertTrue(MiningFixtures.cell(state, 330, 3).reachable);
        assertTrue(engine.connected(state, 331, 3));
        assertEquals(2, state.connectivityVersion);
    }

    @Test void disconnectedBottomOpeningDoesNotScrollUntilItJoinsReachableArea() {
        MiningEngine engine = new MiningEngine();
        MiningState state = MiningFixtures.flat(1, 1001);

        var isolated = engine.dig(state, 8, 6, 103, 1);
        assertEquals(0, isolated.scrollRows());
        assertEquals(1, state.topRow);

        for (int row = 1; row <= 7; row++) {
            MiningState.Cell shaft = MiningFixtures.cell(state, row, 4);
            shaft.hp = 0;
            shaft.reachable = true;
        }
        var connected = engine.dig(state, 7, 5, 103, 2);
        assertEquals(1, connected.scrollRows());
        assertEquals(2, state.topRow);
    }

    @Test void diggingBottomScrollsOnceRejectsOldCoordinatesAndNeverHitsPreGeneratedRows() {
        MiningEngine engine = new MiningEngine();
        MiningState state = MiningFixtures.flat(1, 1001);
        for (int row = 1; row <= 7; row++) {
            MiningState.Cell shaft = MiningFixtures.cell(state, row, 4);
            shaft.hp = 0;
            shaft.reachable = true;
        }
        var first = engine.dig(state, 8, 6, 103, 1);
        assertEquals(4, first.changed().size()); assertEquals(1, first.scrollRows()); assertEquals(2, state.topRow);
        assertTrue(state.cells.stream().noneMatch(c -> c.row == 1));
        assertTrue(state.cells.stream().filter(c -> c.row > 8).allMatch(c -> c.hp > 0));
        assertThrows(MiningException.class, () -> engine.dig(state, 1, 1, 101, 2));
        assertFalse(engine.connected(state, 2, 1));
        assertThrows(MiningException.class, () -> engine.dig(state, 10, 1, 103, 2));
    }

    @Test void resourceRewardAndDepthDoNotAdvanceForPartialDamageOrRepeatedEmptyHit() {
        MiningEngine engine = new MiningEngine();
        MiningState state = MiningFixtures.flat(1, 1004);
        var result = engine.dig(state, 1, 2, 101, 7);
        assertEquals(Map.of(1024037, 1L), result.rewards());
        assertEquals(1, result.rewardCells().size());
        assertEquals(1, result.rewardCells().getFirst().cell().row);
        assertEquals(2, result.rewardCells().getFirst().cell().column);
        assertEquals(1004, result.rewardCells().getFirst().cell().type);
        assertEquals(Map.of(1024037, 1L), result.rewardCells().getFirst().rewards());
        assertEquals(1, state.total.resources.get(1024037));
        assertThrows(MiningException.class, () -> engine.dig(state, 1, 2, 101, 8));
        assertEquals(1, state.total.grids);
        MiningFixtures.cell(state, 8, 1).hp = 2;
        MiningFixtures.cell(state, 7, 1).hp = 0;
        MiningFixtures.cell(state, 7, 1).reachable = true;
        engine.dig(state, 8, 1, 101, 8);
        assertEquals(1, state.topRow); assertEquals(1, state.depth);
    }

    @Test void generatedMapsAreDeterministicBoundedAndUseTableStagesBeyondConfiguredDepth() {
        MiningEngine engine = new MiningEngine();
        MiningState one = engine.create("a", 12345), two = engine.create("a", 12345);
        assertEquals(JSON.toJSONString(one), JSON.toJSONString(two));
        for (MiningState state : List.of(one, two)) {
            for (int row = 1; row <= 7; row++) {
                MiningState.Cell shaft = MiningFixtures.cell(state, row, 3);
                shaft.hp = 0;
                shaft.reachable = true;
            }
        }
        assertEquals(6, one.width); assertEquals(8, one.visibleRows);
        assertEquals(1011, MiningFixtures.cell(one, 2, 1).type);
        assertEquals(1007, MiningFixtures.cell(one, 3, 1).type);
        assertEquals(1008, MiningFixtures.cell(one, 3, 2).type);
        for (int n = 0; n < 220; n++) {
            int bottom = one.topRow + one.visibleRows - 1;
            engine.dig(one, bottom, 3, 103, n + 1);
            assertTrue(one.cells.size() <= (one.visibleRows + 7) * one.width);
            assertEquals(one.cells.size(), one.cells.stream().map(c -> c.row + ":" + c.column).distinct().count());
        }
        assertTrue(one.depth > 192);
        assertTrue(one.cells.stream().allMatch(c -> GameDataManager.getMiningCellTypeCfg(c.type) != null));
    }

    @Test void mapConfigChangeWidensExistingStateAndKeepsOpenedCells() {
        MiningEngine engine = new MiningEngine();
        MiningState state = engine.create("practice", 12345);
        MiningFixtures.cell(state, 1, 1).hp = 0;
        state.width = 2;
        state.cells.removeIf(cell -> cell.column > 2);

        assertTrue(engine.alignToConfig(state));
        assertEquals(6, state.width);
        assertEquals(48, state.cells.stream().filter(cell -> cell.row <= 8).count());
        assertEquals(0, MiningFixtures.cell(state, 1, 1).hp);
        assertFalse(engine.alignToConfig(state));
    }
}
