package com.jjg.game.hall.minigame.game.mining;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sampledata.container.*;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class MiningFixtures {
    static void install() throws Exception {
        Map<Class<? extends BaseCfgBean>, BaseCfgContainer<?>> containers = new LinkedHashMap<>();
        containers.put(MiningCellTypeCfg.class, new MiningCellTypeCfgContainer());
        containers.put(MiningToolsCfg.class, new MiningToolsCfgContainer());
        containers.put(MiningMapGenerationCfg.class, new MiningMapGenerationCfgContainer());
        containers.put(MiningAchievementCfg.class, new MiningAchievementCfgContainer());
        containers.put(MiningBundleShopCfg.class, new MiningBundleShopCfgContainer());
        containers.put(MiningExchangeShopCfg.class, new MiningExchangeShopCfgContainer());
        containers.put(ItemCfg.class, new ItemCfgContainer());
        containers.put(MedalListCfg.class, new MedalListCfgContainer());
        Path samples = Path.of("resources/sample").toAbsolutePath();
        if (!samples.toFile().isDirectory()) samples = Path.of("hall/resources/sample").toAbsolutePath();
        for (BaseCfgContainer<?> container : containers.values()) container.loadData(samples.toString());
        Field field = GameDataManager.class.getDeclaredField("cfgBeanContainerRelator");
        field.setAccessible(true); field.set(GameDataManager.getInstance(), containers);
    }

    static MiningState flat(int hp, int type) {
        MiningState state = new MiningState(); state.seasonId = "practice";
        state.width = 6; state.visibleRows = 8; state.generatedRows = 8; state.seed = 12;
        for (int r = 1; r <= 8; r++) for (int c = 1; c <= 6; c++) state.cells.add(new MiningState.Cell(r, c, type, hp));
        return state;
    }

    static MiningState.Cell cell(MiningState state, int row, int col) {
        return state.cells.stream().filter(c -> c.row == row && c.column == col).findFirst().orElseThrow();
    }
}
