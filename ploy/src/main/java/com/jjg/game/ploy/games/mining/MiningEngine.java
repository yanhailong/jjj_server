package com.jjg.game.ploy.games.mining;

import com.jjg.game.sampledata.bean.*;

import java.util.*;

/** 纯地图规则；调用方先在快照计算，再原子提交消耗、奖励和存档。坐标从1开始。 */
public class MiningEngine {
    private final MiningCatalog catalog;

    public MiningEngine() { this(MiningCatalog.live()); }

    MiningEngine(MiningCatalog catalog) { this.catalog = catalog; }

    public MiningState create(String seasonId, long seed) {
        MiningState state = new MiningState();
        state.seasonId = seasonId;
        state.seed = seed;
        state.width = catalog.width();
        state.visibleRows = catalog.visibleRows();
        ensureRows(state, state.visibleRows);
        return state;
    }

    public record CellReward(MiningState.Cell cell, Map<Integer, Long> rewards) { }
    public record DigResult(int itemId, Map<Integer, Long> rewards, List<CellReward> rewardCells,
                            List<MiningState.Cell> changed, int scrollRows) { }

    public DigResult dig(MiningState state, int row, int column, int toolId, long now) {
        MiningToolsCfg tool = catalog.tool(toolId);
        if (tool == null || tool.getDamage() <= 0 || tool.getItemid() <= 0) throw new MiningException("MISSING_TOOL_CONFIG");
        int bottom = Math.addExact(state.topRow, state.visibleRows - 1);
        if (row < state.topRow || row > bottom || column < 1 || column > state.width) throw new MiningException("OUTSIDE_VISIBLE_MAP");
        MiningState.Cell target = cell(state, row, column);
        if (toolId == catalog.pickToolId()) {
            if (target.hp <= 0) throw new MiningException("CELL_ALREADY_OPEN");
            if (catalog.isWallCell(target.type)) throw new MiningException("WALL_REQUIRES_EXCAVATOR");
            if (!connected(state, row, column)) throw new MiningException("CELL_NOT_CONNECTED");
        }
        List<MiningState.Cell> affected = state.cells.stream()
                .filter(c -> c.row >= state.topRow && c.row <= bottom && c.hp > 0)
                .filter(c -> toolId == catalog.pickToolId() ? c.row == row && c.column == column
                        : toolId == catalog.bombToolId() ? c.row == row && !catalog.isWallCell(c.type)
                        : toolId == catalog.excavatorToolId()
                        && Math.abs(c.row - row) <= 1 && Math.abs(c.column - column) <= 1)
                .toList();
        if (affected.isEmpty()) throw new MiningException("NO_DIGGABLE_CELL");
        Map<Integer, Long> rewards = new HashMap<>();
        List<CellReward> rewardCells = new ArrayList<>();
        boolean scroll = false;
        for (MiningState.Cell c : affected) {
            c.hp = Math.max(0, c.hp - tool.getDamage());
            if (c.hp > 0) continue;
            MiningCellTypeCfg type = catalog.cell(c.type);
            if (type == null) throw new MiningException("MISSING_CELL_CONFIG");
            Map<Integer, Long> cellRewards = MiningCatalog.itemPair(type.getReward());
            merge(rewards, cellRewards);
            if (!cellRewards.isEmpty()) rewardCells.add(new CellReward(c, cellRewards));
            state.total.grids++;
            state.daily.grids++;
            state.daily.depth = Math.max(state.daily.depth, c.row);
            state.total.depth = Math.max(state.total.depth, c.row);
            if (c.row > state.depth) { state.depth = c.row; state.depthReachedAt = now; }
            scroll |= c.row == bottom;
        }
        state.total.tools.merge(tool.getItemid(), 1L, Long::sum);
        state.daily.tools.merge(tool.getItemid(), 1L, Long::sum);
        for (Map.Entry<Integer, Long> e : rewards.entrySet()) {
            if (catalog.resourceItemIds().contains(e.getKey())) {
                state.total.resources.merge(e.getKey(), e.getValue(), Long::sum);
                state.daily.resources.merge(e.getKey(), e.getValue(), Long::sum);
            }
        }
        if (scroll) {
            state.topRow++;
            state.cells.removeIf(c -> c.row < state.topRow);
            state.secrets.keySet().removeIf(id -> state.cells.stream().noneMatch(c -> c.secretId == id));
            ensureRows(state, Math.addExact(bottom, 1));
        }
        return new DigResult(tool.getItemid(), rewards, rewardCells, affected, scroll ? 1 : 0);
    }

    public boolean connected(MiningState state, int row, int column) {
        // 初始地表开放，滚屏后不能把新的顶行误当作地表。
        if (row == 1) return true;
        return state.cells.stream().anyMatch(c -> c.hp == 0 && c.row >= state.topRow
                && Math.abs(c.row - row) + Math.abs(c.column - column) == 1);
    }

    private MiningState.Cell cell(MiningState state, int row, int column) {
        return state.cells.stream().filter(c -> c.row == row && c.column == column).findFirst()
                .orElseThrow(() -> new MiningException("MISSING_MAP_CELL"));
    }

    private void ensureRows(MiningState state, int bottom) {
        while (state.generatedRows < bottom) generateSegment(state);
    }

    private void generateSegment(MiningState state) {
        int first = Math.addExact(state.generatedRows, 1);
        MiningMapGenerationCfg stage = catalog.stages().stream().filter(s -> s.getTotalHeight() >= first)
                .findFirst().orElseGet(() -> catalog.stages().getLast());
        int rows = stage.getHeight();
        if (rows < 1 || rows > 64) throw new MiningException("INVALID_STAGE_HEIGHT");
        int end = Math.addExact(first, rows - 1);
        Random random = new Random(state.seed ^ (first * 0x9E3779B97F4A7C15L));
        Map<Integer, Integer> counts = new HashMap<>();
        Map<Integer, List<Integer>> fixed = new LinkedHashMap<>();
        for (List<Integer> entry : MiningCatalog.entries(stage.getFixedGrid())) {
            int localRow = entry.get(1), column = entry.get(2);
            // 同坐标后写覆盖，表中后配置的格子优先。
            fixed.put((localRow - 1) * state.width + column, entry);
        }
        for (int row = first; row <= end; row++) {
            List<MiningState.Cell> line = new ArrayList<>();
            for (int col = 1; col <= state.width; col++) {
                List<Integer> entry = fixed.get((row - first) * state.width + col);
                // 固定格是阶段表的显式布局，优先级高于格子表中只约束随机掉落的深度范围。
                int selected = entry != null ? entry.get(0) : choose(stage, row, counts, random);
                MiningCellTypeCfg cellType = catalog.cell(selected);
                if (cellType == null) throw new MiningException("MISSING_CELL_CONFIG");
                line.add(new MiningState.Cell(row, col, selected, cellType.getHP()));
                counts.merge(selected, 1, Integer::sum);
            }
            state.cells.addAll(line);
        }
        state.generatedRows = end;
    }

    private int choose(MiningMapGenerationCfg stage, int depth, Map<Integer, Integer> counts, Random random) {
        List<List<Integer>> weights = MiningCatalog.entries(stage.getRandomizedgrid());
        long total = 0;
        for (List<Integer> entry : weights) {
            if (catalog.eligible(entry.get(0), depth) && (entry.get(2) == 0 || counts.getOrDefault(entry.get(0), 0) < entry.get(2)))
                total += entry.get(1);
        }
        if (total == 0) return catalog.defaultCellId();
        long hit = random.nextLong(total);
        for (List<Integer> entry : weights) {
            if (!catalog.eligible(entry.get(0), depth) || (entry.get(2) > 0 && counts.getOrDefault(entry.get(0), 0) >= entry.get(2))) continue;
            hit -= entry.get(1);
            if (hit < 0) return entry.get(0);
        }
        throw new IllegalStateException("Unreachable weight selection");
    }

    Set<Integer> toolItemIds() { return catalog.toolItemIds(); }
    Set<Integer> resourceItemIds() { return catalog.resourceItemIds(); }
    public static void merge(Map<Integer, Long> into, Map<Integer, Long> values) {
        values.forEach((id, count) -> into.merge(id, count, Math::addExact));
    }
}
