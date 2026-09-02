package com.jjg.game.sim.service;

import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimBuildingRedDotTest {
    @Test void skillRedDotUsesLatestConfiguredCostWithoutResearchPointTypeRestriction() {
        SimSkillService service = spy(new SimSkillService());
        SimPlayerContext ctx = new SimPlayerContext();
        SimSkillsData skillData = new SimSkillsData();
        skillData.setGameType(10); skillData.changeSkillLevel(201, 1);
        ctx.getSkillsDataMap().put(10, skillData);
        BuildingData building = new BuildingData(); building.setLevel(2);
        SimCasinoData casino = new SimCasinoData(); casino.setBuildingData(Map.of(101, building));
        ctx.setCurrentCasino(casino);
        SimConfigCacheService config = mock(SimConfigCacheService.class);
        BuildingAreaTableCfg area = mock(BuildingAreaTableCfg.class); when(area.getId()).thenReturn(101);
        when(config.getBuildingAreaTableCfgByGameType(10)).thenReturn(area);
        ReflectionTestUtils.setField(service, "simConfigCacheService", config);
        var next = mock(com.jjg.game.sampledata.bean.ResearchSkillsCfg.class);
        when(next.getResearchPoints()).thenReturn(Map.of(9001, 4));
        doReturn(next).when(service).getResearchSkillsCfg(10, 201, 2);
        var prop = mock(com.jjg.game.sampledata.bean.PropCfg.class);
        when(prop.getSkillTypeId()).thenReturn(2); when(prop.getGameType()).thenReturn(10);
        try (var configs = mockStatic(GameDataManager.class)) {
            configs.when(() -> GameDataManager.getPropCfg(201)).thenReturn(prop);
            assertEquals(Map.of(9001, 4L), service.redDotUpgradeCost(ctx, 10, 201));
        }
        assertEquals(1, skillData.findSkilLevelByPropId(201).getLevel());
    }

    @Test void buildingAndSkillCountSeparatelyAndPackSnapshotIsCached() {
        SimBuildingRedDotService service = new SimBuildingRedDotService();
        SimPlayerContext ctx = new SimPlayerContext();
        PlayerController controller = mock(PlayerController.class); Player player = mock(Player.class);
        when(controller.playerId()).thenReturn(1L); when(controller.getPlayer()).thenReturn(player);
        ctx.setPlayerController(controller);
        SimCasinoData casino = new SimCasinoData(); casino.setCasinoId(1);
        casino.setBuildingData(Map.of(101, new BuildingData())); ctx.setCurrentCasino(casino);
        SimSkillsData data = new SimSkillsData(); data.setGameType(10); data.changeSkillLevel(201, 1);
        ctx.getSkillsDataMap().put(10, data);
        SimPlayerContextRegistry contexts = mock(SimPlayerContextRegistry.class); when(contexts.getContext(1)).thenReturn(ctx);
        SimBuildingService buildings = mock(SimBuildingService.class); SimSkillService skills = mock(SimSkillService.class);
        when(buildings.redDotUpgradeCost(ctx, 101)).thenReturn(Map.of(100, 1L));
        when(skills.redDotUpgradeCost(ctx, 10, 201)).thenReturn(Map.of(100, 1L));
        PlayerPackService packs = mock(PlayerPackService.class);
        when(packs.findSatisfiedItemRequirements(eq(player), anyList())).thenReturn(Set.of(0, 1));
        RedDotManager dots = spy(new RedDotManager(null, null, null)); doNothing().when(dots).updateRedDot(anyList(), anyLong());
        ReflectionTestUtils.setField(service, "contexts", contexts); ReflectionTestUtils.setField(service, "buildings", buildings);
        ReflectionTestUtils.setField(service, "skills", skills); ReflectionTestUtils.setField(service, "packs", packs);
        ReflectionTestUtils.setField(service, "manager", dots);
        BuildingAreaTableCfg cfg = mock(BuildingAreaTableCfg.class); when(cfg.getUnlockGameId()).thenReturn(10);
        try (var configs = mockStatic(GameDataManager.class)) {
            configs.when(() -> GameDataManager.getBuildingAreaTableCfg(101)).thenReturn(cfg);
            var dot = service.initialize(1, 1).getFirst();
            assertEquals(2, dot.getCount());
            assertTrue(dot.getExtra().contains("buildingIds")); assertTrue(dot.getExtra().contains("201"));
            service.onTick(ctx, System.currentTimeMillis());
            verify(packs, times(1)).findSatisfiedItemRequirements(eq(player), anyList());
            service.invalidate(1);
            when(packs.findSatisfiedItemRequirements(eq(player), anyList())).thenReturn(Set.of());
            service.onTick(ctx, System.currentTimeMillis());
            assertEquals(0, service.initialize(1, 1).getFirst().getCount());
        }
        verify(buildings, never()).onUpgradeBuilding(any(), anyInt());
        verify(skills, never()).onUpgradeSkill(any(), anyInt(), anyInt());
    }

    @Test void globalAndGameSpecificSkillUpgradesMergeIntoOneCount() {
        SimBuildingRedDotService service = new SimBuildingRedDotService();
        SimPlayerContext ctx = new SimPlayerContext();
        PlayerController controller = mock(PlayerController.class);
        Player player = mock(Player.class);
        when(controller.playerId()).thenReturn(2L);
        when(controller.getPlayer()).thenReturn(player);
        ctx.setPlayerController(controller);
        SimCasinoData casino = new SimCasinoData();
        casino.setCasinoId(1);
        casino.setBuildingData(Map.of(101, new BuildingData(), 102, new BuildingData()));
        ctx.setCurrentCasino(casino);
        SimSkillsData global = new SimSkillsData();
        global.setGameType(0);
        global.changeSkillLevel(201, 1);
        SimSkillsData specific = new SimSkillsData();
        specific.setGameType(10);
        specific.changeSkillLevel(202, 1);
        ctx.getSkillsDataMap().put(0, global);
        ctx.getSkillsDataMap().put(10, specific);

        SimPlayerContextRegistry contexts = mock(SimPlayerContextRegistry.class);
        when(contexts.getContext(2L)).thenReturn(ctx);
        SimBuildingService buildings = mock(SimBuildingService.class);
        SimSkillService skills = mock(SimSkillService.class);
        when(buildings.redDotUpgradeCost(ctx, 101)).thenReturn(null);
        when(buildings.redDotUpgradeCost(ctx, 102)).thenReturn(null);
        when(skills.redDotUpgradeCost(ctx, 0, 201)).thenReturn(Map.of(100, 1L));
        when(skills.redDotUpgradeCost(ctx, 10, 202)).thenReturn(Map.of(100, 1L));
        PlayerPackService packs = mock(PlayerPackService.class);
        when(packs.findSatisfiedItemRequirements(eq(player), anyList())).thenReturn(Set.of(0, 1));
        ReflectionTestUtils.setField(service, "contexts", contexts);
        ReflectionTestUtils.setField(service, "buildings", buildings);
        ReflectionTestUtils.setField(service, "skills", skills);
        ReflectionTestUtils.setField(service, "packs", packs);
        ReflectionTestUtils.setField(service, "manager", new RedDotManager(null, null, null));
        BuildingAreaTableCfg globalCfg = mock(BuildingAreaTableCfg.class);
        BuildingAreaTableCfg specificCfg = mock(BuildingAreaTableCfg.class);
        when(globalCfg.getUnlockGameId()).thenReturn(0);
        when(specificCfg.getUnlockGameId()).thenReturn(10);

        try (var configs = mockStatic(GameDataManager.class)) {
            configs.when(() -> GameDataManager.getBuildingAreaTableCfg(101)).thenReturn(globalCfg);
            configs.when(() -> GameDataManager.getBuildingAreaTableCfg(102)).thenReturn(specificCfg);
            var dot = service.initialize(2L, 1).getFirst();
            assertEquals(1, dot.getCount());
            assertTrue(dot.getExtra().contains("\"skillCount\":1"));
            assertTrue(dot.getExtra().contains("201"));
            assertTrue(dot.getExtra().contains("202"));
        }
    }
}
