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
        state.connectivityVersion = 2;
        ensureRows(state, state.visibleRows);
        return state;
    }

    boolean matchesConfig(MiningState state) {
        return state.width == catalog.width() && state.visibleRows == catalog.visibleRows();
    }

    /** 配置宽高变化时重建地图，并保留旧地图中仍有效坐标的开采状态。 */
    boolean alignToConfig(MiningState state) {
        if (matchesConfig(state)) return false;
        int oldTopRow = Math.max(1, state.topRow);
        int oldGeneratedRows = Math.max(state.generatedRows,
                Math.addExact(oldTopRow, catalog.visibleRows() - 1));
        Map<Long, MiningState.Cell> oldCells = new HashMap<>();
        if (state.cells != null) {
            for (MiningState.Cell cell : state.cells) {
                if (cell != null && cell.row >= oldTopRow && cell.column >= 1
                        && cell.column <= catalog.width() && catalog.cell(cell.type) != null && cell.hp >= 0) {
                    oldCells.put(cellKey(cell.row, cell.column), cell);
                }
            }
        }

        MiningState rebuilt = create(state.seasonId, state.seed);
        ensureRows(rebuilt, oldGeneratedRows);
        for (MiningState.Cell cell : rebuilt.cells) {
            MiningState.Cell old = oldCells.get(cellKey(cell.row, cell.column));
            if (old == null) continue;
            cell.type = old.type;
            cell.hp = old.hp;
            cell.secretId = old.secretId;
            cell.reachable = old.reachable;
        }
        rebuilt.cells.removeIf(cell -> cell.row < oldTopRow);

        state.width = rebuilt.width;
        state.visibleRows = rebuilt.visibleRows;
        state.topRow = oldTopRow;
        state.generatedRows = rebuilt.generatedRows;
        state.cells = rebuilt.cells;
        if (state.secrets == null) state.secrets = new HashMap<>();
        else state.secrets.keySet().removeIf(id -> state.cells.stream().noneMatch(cell -> cell.secretId == id));
        return true;
    }

    /**
     * 兼容旧存档：首屏可从真实地表精确恢复；已滚屏但丢失连通前沿的存档，
     * 以当前顶行已打开区域作为历史连通入口。版本1已有的有效前沿继续保留。
     */
    boolean alignConnectivity(MiningState state) {
        if (state.connectivityVersion >= 2) return false;
        if (state.cells == null) state.cells = new ArrayList<>();
        if (state.connectivityVersion < 1) {
            state.cells.forEach(cell -> cell.reachable = false);
        } else {
            state.cells.stream().filter(cell -> cell.hp > 0).forEach(cell -> cell.reachable = false);
        }
        boolean hasReachableOpenCell = state.cells.stream().anyMatch(cell -> cell.hp == 0 && cell.reachable);
        if (!hasReachableOpenCell) {
            int rootRow = state.topRow <= 1 ? 1 : state.topRow;
            state.cells.stream().filter(cell -> cell.row == rootRow && cell.hp == 0)
                    .forEach(cell -> cell.reachable = true);
        }
        refreshReachability(state);
        state.connectivityVersion = 2;
        return true;
    }

    private static long cellKey(int row, int column) {
        return ((long) row << 32) ^ (column & 0xffffffffL);
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
        int previousReachableDepth = deepestReachableDepth(state, state.topRow - 1, bottom);
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
        }
        refreshReachability(state);
        // 炸弹/挖机可在未连通区域制造空洞；只有空洞真正接入地表后才允许触底滚动，
        // 否则会过早删除上方连通前沿，最终令整张可视地图都不可挖。
        int reachableDepth = deepestReachableDepth(state, state.topRow - 1, bottom);
        int scrollRows = reachableDepth == bottom
                ? Math.min(state.visibleRows - 1, Math.max(1, reachableDepth - previousReachableDepth)) : 0;
        state.total.tools.merge(tool.getItemid(), 1L, Long::sum);
        state.daily.tools.merge(tool.getItemid(), 1L, Long::sum);
        for (Map.Entry<Integer, Long> e : rewards.entrySet()) {
            if (catalog.resourceItemIds().contains(e.getKey())) {
                state.total.resources.merge(e.getKey(), e.getValue(), Long::sum);
                state.daily.resources.merge(e.getKey(), e.getValue(), Long::sum);
            }
        }
        if (scrollRows > 0) {
            state.topRow = Math.addExact(state.topRow, scrollRows);
            state.cells.removeIf(c -> c.row < state.topRow);
            state.secrets.keySet().removeIf(id -> state.cells.stream().noneMatch(c -> c.secretId == id));
            ensureRows(state, Math.addExact(bottom, scrollRows));
            // 阶段会整段预生成；只在新可视窗口内延伸连通前沿，不能提前穿透屏幕外空格。
            refreshReachability(state);
        }
        return new DigResult(tool.getItemid(), rewards, rewardCells, affected, scrollRows);
    }

    private static int deepestReachableDepth(MiningState state, int defaultDepth, int visibleBottom) {
        return state.cells.stream().filter(c -> c.hp == 0 && c.reachable && c.row <= visibleBottom)
                .mapToInt(c -> c.row).max().orElse(defaultDepth);
    }

    public boolean connected(MiningState state, int row, int column) {
        // 未挖的第一行直接暴露在地表；其他格必须邻接真正连到地表的开放格。
        if (row == 1) return true;
        int bottom = Math.addExact(state.topRow, state.visibleRows - 1);
        return state.cells.stream().anyMatch(c -> c.hp == 0 && c.reachable && c.row >= state.topRow
                && c.row <= bottom
                && Math.abs(c.row - row) + Math.abs(c.column - column) == 1);
    }

    private void refreshReachability(MiningState state) {
        int bottom = Math.addExact(state.topRow, state.visibleRows - 1);
        state.cells.stream().filter(cell -> cell.hp > 0 || cell.row > bottom)
                .forEach(cell -> cell.reachable = false);
        ArrayDeque<MiningState.Cell> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        for (MiningState.Cell cell : state.cells) {
            if (cell.row >= state.topRow && cell.row <= bottom && cell.hp == 0
                    && (cell.reachable || cell.row == 1)) {
                cell.reachable = true;
                if (visited.add(cellKey(cell.row, cell.column))) queue.add(cell);
            }
        }
        Map<Long, MiningState.Cell> opened = new HashMap<>();
        state.cells.stream().filter(cell -> cell.row >= state.topRow && cell.row <= bottom && cell.hp == 0)
                .forEach(cell -> opened.put(cellKey(cell.row, cell.column), cell));
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            MiningState.Cell current = queue.removeFirst();
            for (int[] direction : directions) {
                MiningState.Cell neighbor = opened.get(cellKey(current.row + direction[0], current.column + direction[1]));
                if (neighbor == null || !visited.add(cellKey(neighbor.row, neighbor.column))) continue;
                neighbor.reachable = true;
                queue.addLast(neighbor);
            }
        }
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
    int pickItemId() { return catalog.pickItemId(); }
    Set<Integer> resourceItemIds() { return catalog.resourceItemIds(); }
    public static void merge(Map<Integer, Long> into, Map<Integer, Long> values) {
        values.forEach((id, count) -> into.merge(id, count, Math::addExact));
    }
}
