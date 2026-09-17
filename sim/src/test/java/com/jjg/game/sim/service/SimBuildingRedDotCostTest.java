package com.jjg.game.sim.service;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.BuildingUpgradeTableCfg;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sampledata.bean.PropCfg;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SimSkillsData;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class SimBuildingRedDotCostTest {
    @Test
    void buildingsWithoutSkillEntryDoNotInheritGlobalSkillRedDot() {
        BuildingAreaTableCfg area = mock(BuildingAreaTableCfg.class);
        when(area.getUnlockGameId()).thenReturn(0, 100);
        assertFalse(SimBuildingRedDotService.hasSkillEntry(area));
        assertTrue(SimBuildingRedDotService.hasSkillEntry(area));
        assertFalse(SimBuildingRedDotService.hasSkillEntry(null));
    }

    @Test
    void maxedOrHiddenSkillDoesNotOfferRedDot() {
        int propId = 123;
        SimPlayerContext ctx = new SimPlayerContext();
        SimSkillsData data = new SimSkillsData();
        data.setGameType(0);
        data.changeSkillLevel(propId, 0);
        ctx.getSkillsDataMap().put(0, data);

        PropCfg prop = mock(PropCfg.class);
        when(prop.getSkillTypeId()).thenReturn(1);
        ResearchSkillsCfg next = mock(ResearchSkillsCfg.class);
        when(next.getResearchPoints()).thenReturn(Map.of(456, 2));
        SimSkillService service = new SimSkillService();
        ReflectionTestUtils.setField(service, "skillsCfgMap", Map.of(0, Map.of(propId, Map.of(1, next))));

        try (var gameData = mockStatic(GameDataManager.class)) {
            gameData.when(() -> GameDataManager.getPropCfg(propId)).thenReturn(prop);
            assertEquals(Map.of(456, 2L), service.redDotUpgradeCost(ctx, 0, propId));

            data.changeSkillLevel(propId, 1);
            assertNull(service.redDotUpgradeCost(ctx, 0, propId));

            data.changeSkillLevel(propId, 0);
            when(prop.getSortOrder()).thenReturn(-1);
            assertNull(service.redDotUpgradeCost(ctx, 0, propId));
        }
    }

    @Test
    void upgradeEntryDoesNotAddSkillReminderToBuildingCount() {
        assertEquals(1, SimBuildingRedDotService.upgradeEntryCount(1, 1));
        assertEquals(2, SimBuildingRedDotService.upgradeEntryCount(2, 1));
        assertEquals(1, SimBuildingRedDotService.upgradeEntryCount(0, 1));
        assertEquals(0, SimBuildingRedDotService.upgradeEntryCount(0, 0));
    }

    @Test
    void diamondShortageDoesNotCountAsAffordableUpgrade() {
        int diamondId = 1990001;
        Player player = mock(Player.class);
        when(player.getId()).thenReturn(7L);
        when(player.getDiamond()).thenReturn(9L, 10L);
        ItemCfg diamond = mock(ItemCfg.class);
        when(diamond.getType()).thenReturn(GameConstant.Item.TYPE_DIAMOND);
        PlayerPackService packs = spy(new PlayerPackService());
        doReturn(mock(PlayerPack.class)).when(packs).getFromAllDB(7L);

        try (var gameData = mockStatic(GameDataManager.class)) {
            gameData.when(() -> GameDataManager.getItemCfg(diamondId)).thenReturn(diamond);
            List<Map<Integer, Long>> costs = List.of(Map.of(diamondId, 10L));
            assertEquals(Set.of(), packs.findSatisfiedItemRequirements(player, costs));
            assertEquals(Set.of(0), packs.findSatisfiedItemRequirements(player, costs));
        }
    }

    @Test
    void requiresCasinoLevelAndAllRemainingUpgradeCosts() {
        int buildingId = 1101;
        int goldId = 1990000;
        int diamondId = 1990001;
        int materialId = 1024005;
        BuildingData building = new BuildingData();
        building.setId(buildingId);
        building.setLevel(1);
        SimCasinoData casino = new SimCasinoData();
        casino.setBuildingData(Map.of(buildingId, building));
        SimBaseData base = new SimBaseData();
        SimPlayerContext ctx = new SimPlayerContext();
        ctx.setCurrentCasino(casino);
        ctx.setSimBaseData(base);
        Player player = mock(Player.class);
        PlayerController controller = mock(PlayerController.class);
        when(controller.getPlayer()).thenReturn(player);
        when(player.getLevel()).thenReturn(1, 2);
        ctx.setPlayerController(controller);

        BuildingUpgradeTableCfg current = mock(BuildingUpgradeTableCfg.class);
        when(current.getNeedLevel()).thenReturn(5);
        when(current.getUpgradeCost()).thenReturn(Map.of(goldId, 100L, diamondId, 10L, materialId, 2L));
        when(current.getCostPerLevel()).thenReturn(List.of(
                List.of(materialId, 3), List.of(materialId, 4)));
        SimConfigCacheService configs = mock(SimConfigCacheService.class);
        when(configs.getBuildingUpgradeCfg(buildingId, 1)).thenReturn(current);
        when(configs.getBuildingUpgradeCfg(buildingId, 2)).thenReturn(mock(BuildingUpgradeTableCfg.class));
        SimBuildingService service = new SimBuildingService();
        ReflectionTestUtils.setField(service, "configCache", configs);

        try (var gameData = mockStatic(GameDataManager.class)) {
            base.setAllLevel(5);
            assertNull(service.redDotUpgradeCost(ctx, buildingId)); // 玩家等级不足

            base.setAllLevel(4);
            assertNull(service.redDotUpgradeCost(ctx, buildingId)); // 经营等级不足

            base.setAllLevel(5);
            building.setCdEndTime(System.currentTimeMillis() - 1);
            assertNull(service.redDotUpgradeCost(ctx, buildingId)); // CD 到点但未结算
            building.setCdEndTime(0);
            assertEquals(Map.of(goldId, 100L, diamondId, 10L, materialId, 9L),
                    service.redDotUpgradeCost(ctx, buildingId));

            building.setProgress(1);
            assertEquals(Map.of(goldId, 100L, diamondId, 10L, materialId, 6L),
                    service.redDotUpgradeCost(ctx, buildingId));

            building.setProgress(2);
            assertEquals(Map.of(goldId, 100L, diamondId, 10L, materialId, 2L),
                    service.redDotUpgradeCost(ctx, buildingId));
        }
    }
}
