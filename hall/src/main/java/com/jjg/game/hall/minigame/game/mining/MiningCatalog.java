package com.jjg.game.hall.minigame.game.mining;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.MiningCellTypeCfg;
import com.jjg.game.sampledata.bean.MiningMapGenerationCfg;
import com.jjg.game.sampledata.bean.MiningToolsCfg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 对六张 Mining 配置表做一次语义校验，并向玩法提供不依赖具体配置 ID 的查询。 */
final class MiningCatalog {
    private final Map<Integer, MiningCellTypeCfg> cells;
    private final Map<Integer, MiningToolsCfg> tools;
    private final List<MiningMapGenerationCfg> stages;
    private final Set<Integer> toolItemIds;
    private final Set<Integer> resourceItemIds;
    private final Set<Integer> wallCellIds;
    private final int pickToolId;
    private final int bombToolId;
    private final int excavatorToolId;
    private final int defaultCellId;
    private final int width;
    private final int visibleRows;

    static MiningCatalog live() {
        return new MiningCatalog(GameDataManager.getMiningCellTypeCfgMap(),
                GameDataManager.getMiningToolsCfgMap(), GameDataManager.getMiningMapGenerationCfgList());
    }

    MiningCatalog(Map<Integer, MiningCellTypeCfg> cells, Map<Integer, MiningToolsCfg> tools,
                  List<MiningMapGenerationCfg> stages) {
        if (cells == null || cells.isEmpty() || tools == null || tools.size() != 3 || stages == null || stages.isEmpty()) {
            throw new MiningException("MISSING_MINING_CONFIG");
        }
        this.cells = Map.copyOf(cells);
        this.tools = Map.copyOf(tools);
        this.stages = stages.stream().sorted(Comparator.comparingInt(MiningMapGenerationCfg::getTotalHeight)).toList();

        List<MiningToolsCfg> orderedTools = tools.values().stream()
                .sorted(Comparator.comparingInt(MiningToolsCfg::getDamage).thenComparingInt(MiningToolsCfg::getId)).toList();
        if (orderedTools.stream().anyMatch(t -> t.getDamage() <= 0 || t.getItemid() <= 0)
                || orderedTools.get(0).getDamage() >= orderedTools.get(1).getDamage()
                || orderedTools.get(1).getDamage() >= orderedTools.get(2).getDamage()) {
            throw new MiningException("INVALID_TOOL_CONFIG");
        }
        pickToolId = orderedTools.get(0).getId();
        bombToolId = orderedTools.get(1).getId();
        excavatorToolId = orderedTools.get(2).getId();
        toolItemIds = orderedTools.stream().map(MiningToolsCfg::getItemid)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        Set<Integer> resources = new LinkedHashSet<>();
        List<MiningCellTypeCfg> obstacles = new ArrayList<>();
        for (MiningCellTypeCfg cell : cells.values()) {
            if (cell.getHP() < 0) throw new MiningException("INVALID_CELL_CONFIG");
            Map<Integer, Long> reward = itemPair(cell.getReward());
            if (reward.isEmpty() && cell.getHP() > 0) obstacles.add(cell);
            reward.keySet().stream().filter(id -> !toolItemIds.contains(id)).forEach(resources::add);
            List<Integer> depth = cell.getDropDepth();
            if (depth != null && !depth.isEmpty()
                    && (depth.size() != 2 || depth.get(0) < 0 || depth.get(0) > depth.get(1))) {
                throw new MiningException("INVALID_CELL_DEPTH");
            }
        }
        if (obstacles.isEmpty()) throw new MiningException("MISSING_OBSTACLE_CELL");
        defaultCellId = obstacles.stream().min(Comparator.comparingInt(MiningCellTypeCfg::getHP)
                .thenComparingInt(MiningCellTypeCfg::getId)).orElseThrow().getId();
        int bombDamage = tools.get(bombToolId).getDamage();
        Set<Integer> walls = new HashSet<>();
        obstacles.stream().filter(c -> c.getHP() > bombDamage).map(MiningCellTypeCfg::getId).forEach(walls::add);
        wallCellIds = Set.copyOf(walls);
        resourceItemIds = Set.copyOf(resources);

        int configuredWidth = 0;
        int expectedTotalHeight = 0;
        for (MiningMapGenerationCfg stage : this.stages) {
            if (stage.getHeight() <= 0 || stage.getHeight() > 64 || stage.getTotalHeight() <= 0) {
                throw new MiningException("INVALID_STAGE_HEIGHT");
            }
            expectedTotalHeight = Math.addExact(expectedTotalHeight, stage.getHeight());
            if (stage.getTotalHeight() != expectedTotalHeight) throw new MiningException("INVALID_STAGE_RANGE");
            for (List<Integer> entry : entries(stage.getFixedGrid())) {
                configuredWidth = Math.max(configuredWidth, entry.get(2));
            }
            for (List<Integer> entry : entries(stage.getRandomizedgrid())) {
                if (!cells.containsKey(entry.get(0)) || entry.get(1) <= 0 || entry.get(2) < 0) {
                    throw new MiningException("INVALID_RANDOM_GRID");
                }
            }
        }
        if (configuredWidth <= 0) throw new MiningException("MISSING_MAP_WIDTH");
        width = configuredWidth;
        visibleRows = this.stages.get(0).getHeight();
        for (MiningMapGenerationCfg stage : this.stages) {
            for (List<Integer> entry : entries(stage.getFixedGrid())) {
                if (!cells.containsKey(entry.get(0)) || entry.get(1) < 1 || entry.get(1) > stage.getHeight()
                        || entry.get(2) < 1 || entry.get(2) > width) {
                    throw new MiningException("INVALID_FIXED_GRID");
                }
            }
        }
    }

    MiningCellTypeCfg cell(int id) { return cells.get(id); }
    MiningToolsCfg tool(int id) { return tools.get(id); }
    List<MiningMapGenerationCfg> stages() { return stages; }
    int pickToolId() { return pickToolId; }
    int bombToolId() { return bombToolId; }
    int excavatorToolId() { return excavatorToolId; }
    int defaultCellId() { return defaultCellId; }
    int width() { return width; }
    int visibleRows() { return visibleRows; }
    Set<Integer> toolItemIds() { return toolItemIds; }
    Set<Integer> resourceItemIds() { return resourceItemIds; }
    boolean isWallCell(int id) { return wallCellIds.contains(id); }

    boolean eligible(int id, int depth) {
        MiningCellTypeCfg cfg = cells.get(id);
        if (cfg == null) return false;
        List<Integer> range = cfg.getDropDepth();
        return range == null || range.isEmpty() || depth >= range.get(0) && depth <= range.get(1);
    }

    static List<List<Integer>> entries(List<List<Integer>> values) {
        if (values == null || values.isEmpty()) return List.of();
        for (List<Integer> value : values) {
            if (value == null || value.size() != 3) throw new MiningException("INVALID_MAP_ENTRY");
        }
        return values;
    }

    static Map<Integer, Long> itemPair(List<Integer> values) {
        if (values == null || values.isEmpty()) return Map.of();
        if (values.size() != 2 || values.get(0) <= 0 || values.get(1) <= 0) {
            throw new MiningException("INVALID_ITEM_PAIR");
        }
        return Map.of(values.get(0), values.get(1).longValue());
    }
}
