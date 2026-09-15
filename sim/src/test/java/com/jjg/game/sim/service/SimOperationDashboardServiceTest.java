package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.BuildingUpgradeTableCfg;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.BuildingType;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.pb.res.ResOperationCapacity;
import com.jjg.game.sim.pb.res.ResOperationDashboard;
import com.jjg.game.sim.pb.struct.OperationBuildingData;
import com.jjg.game.sim.pb.struct.OperationDashboardOverview;
import com.jjg.game.sim.pb.struct.OperationResearchBuilding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimOperationDashboardServiceTest {

    /** 单栋建筑的产出必须与看板总览、建筑详情保持同一小时口径。 */
    @Test
    void shouldConvertBuildingOutputFromMinuteToHour() {
        SimOperationDashboardService service = new SimOperationDashboardService();
        SimBuildingService buildingService = mock(SimBuildingService.class);
        SimConfigCacheService configCache = mock(SimConfigCacheService.class);
        ReflectionTestUtils.setField(service, "buildingService", buildingService);
        ReflectionTestUtils.setField(service, "configCache", configCache);

        SimPlayerContext ctx = mock(SimPlayerContext.class);
        SimCasinoData casino = mock(SimCasinoData.class);
        BuildingData building = mock(BuildingData.class);
        BuildingAreaTableCfg areaCfg = mock(BuildingAreaTableCfg.class);
        BuildingUpgradeTableCfg levelCfg = mock(BuildingUpgradeTableCfg.class);
        when(ctx.getCurrentCasino()).thenReturn(casino);
        when(casino.getCasinoId()).thenReturn(1);
        when(casino.getBuildingData()).thenReturn(Map.of(1101, building));
        when(casino.findBuilding(1101)).thenReturn(building);
        when(building.getId()).thenReturn(1101);
        when(building.getLevel()).thenReturn(5);
        when(areaCfg.getId()).thenReturn(1101);
        when(areaCfg.getRegionID()).thenReturn(1);
        when(areaCfg.getType()).thenReturn(BuildingType.GAME.getCode());
        when(configCache.getBuildingUpgradeCfg(1101, 5)).thenReturn(levelCfg);
        when(buildingService.computeDashboardBuildingValues(ctx, building)).thenReturn(Map.of(
                BuildingOutputType.GOLD, 300L,
                BuildingOutputType.CASINO_LEVEL_EXP, 20L,
                BuildingOutputType.POWER, 2L));

        List<OperationBuildingData> buildings;
        try (MockedStatic<GameDataManager> configs = mockStatic(GameDataManager.class)) {
            configs.when(GameDataManager::getBuildingAreaTableCfgList).thenReturn(List.of(areaCfg));
            configs.when(() -> GameDataManager.getBuildingAreaTableCfg(1101)).thenReturn(areaCfg);
            buildings = ReflectionTestUtils.invokeMethod(service, "buildBuildingData",
                    ctx, casino, new OperationDashboardOverview(), System.currentTimeMillis());
        }

        assertEquals(1, buildings.size());
        assertEquals(18_000L, buildings.get(0).goldOutputPerMinute);
        assertEquals(1_200L, buildings.get(0).expOutputPerMinute);
        assertEquals(120L, buildings.get(0).powerOutputPerMinute);
    }

    /** 满意度标准改为所有已解锁建筑当前等级 InteractCount 之和除以100。 */
    @Test
    void shouldUseBuildingInteractionCountForSatisfactionAndReceptionWarning() {
        SimOperationDashboardService service = new SimOperationDashboardService();
        SimBuildingService buildingService = mock(SimBuildingService.class);
        SimConfigCacheService configCache = mock(SimConfigCacheService.class);
        ReflectionTestUtils.setField(service, "buildingService", buildingService);
        ReflectionTestUtils.setField(service, "configCache", configCache);

        SimPlayerContext ctx = mock(SimPlayerContext.class);
        SimCasinoData casino = mock(SimCasinoData.class);
        when(ctx.getCurrentCasino()).thenReturn(casino);
        when(casino.getCasinoId()).thenReturn(1);
        when(casino.getBuildingData()).thenReturn(Map.of());
        when(casino.countGenerateInWindow(anyLong(), anyLong())).thenReturn(10);
        when(casino.countInteractionsInWindow(anyLong(), anyLong())).thenReturn(1);
        when(buildingService.computePerMinuteOutput(ctx, casino)).thenReturn(Map.of());
        when(buildingService.computeDeptValue(ctx, casino,
                com.jjg.game.sim.constant.BuildingOutputType.SERVICE)).thenReturn(100L);
        when(buildingService.computeStandardInteractionCount(casino)).thenReturn(25);
        when(buildingService.computeProsperity(casino)).thenReturn(600);

        try (MockedStatic<GameDataManager> configs = mockStatic(GameDataManager.class)) {
            configs.when(GameDataManager::getBuildingAreaTableCfgList).thenReturn(List.of());
            service.onDashboard(ctx);
        }

        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(ctx).send(message.capture());
        ResOperationDashboard dashboard = (ResOperationDashboard) message.getValue();
        assertEquals(4_000, dashboard.overview.satisfactionRate);
        assertEquals(25, dashboard.overview.standardInteractionCount);
        assertEquals(600, dashboard.overview.totalProsperity);
        assertTrue(dashboard.overview.receptionIncomeTooLow);
        assertTrue(dashboard.overview.hasWarning);
        assertEquals(4_062_133, dashboard.overview.promptLanguageId);
    }

    /** 游戏建筑最近窗口实际人数低于当前容纳上限的60%时，显示机台不足和入口感叹号。 */
    @Test
    void shouldWarnWhenGameCapacityUsageIsBelowConfiguredThreshold() {
        SimOperationDashboardService service = new SimOperationDashboardService();
        SimBuildingService buildingService = mock(SimBuildingService.class);
        SimConfigCacheService configCache = mock(SimConfigCacheService.class);
        ReflectionTestUtils.setField(service, "buildingService", buildingService);
        ReflectionTestUtils.setField(service, "configCache", configCache);

        SimPlayerContext ctx = mock(SimPlayerContext.class);
        SimBaseData baseData = mock(SimBaseData.class);
        SimCasinoData casino = mock(SimCasinoData.class);
        BuildingData building = mock(BuildingData.class);
        BuildingAreaTableCfg areaCfg = mock(BuildingAreaTableCfg.class);
        BuildingUpgradeTableCfg levelCfg = mock(BuildingUpgradeTableCfg.class);
        when(ctx.getCurrentCasino()).thenReturn(casino);
        when(ctx.getSimBaseData()).thenReturn(baseData);
        when(casino.getCasinoId()).thenReturn(1);
        when(casino.getBuildingData()).thenReturn(Map.of(1101, building));
        when(casino.findBuilding(1101)).thenReturn(building);
        when(casino.countBuildingInteractionsInWindow(eq(1101), anyLong(), anyLong())).thenReturn(5);
        when(building.getId()).thenReturn(1101);
        when(building.getLevel()).thenReturn(1);
        when(areaCfg.getId()).thenReturn(1101);
        when(areaCfg.getRegionID()).thenReturn(1);
        when(areaCfg.getType()).thenReturn(BuildingType.GAME.getCode());
        when(levelCfg.getMaxInteractionCount()).thenReturn(10);
        when(configCache.getBuildingUpgradeCfg(1101, 1)).thenReturn(levelCfg);
        when(buildingService.computePerMinuteOutput(ctx, casino)).thenReturn(Map.of());
        when(buildingService.computeDashboardBuildingValues(ctx, building)).thenReturn(Map.of());

        try (MockedStatic<GameDataManager> configs = mockStatic(GameDataManager.class)) {
            configs.when(GameDataManager::getBuildingAreaTableCfgList).thenReturn(List.of(areaCfg));
            configs.when(() -> GameDataManager.getBuildingAreaTableCfg(1101)).thenReturn(areaCfg);
            service.onDashboard(ctx);
        }

        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(ctx).send(message.capture());
        ResOperationDashboard dashboard = (ResOperationDashboard) message.getValue();
        assertTrue(dashboard.overview.hasWarning);
        assertTrue(dashboard.buildings.get(0).incomeTooLow);
        assertEquals(4_062_137, dashboard.buildings.get(0).warningLanguageId);
    }

    /** 研发状态必须按经营等级顺序判定，不能受配置容器的无序遍历影响。 */
    @Test
    void shouldChooseCurrentResearchByCasinoLevelOrder() {
        SimOperationDashboardService service = new SimOperationDashboardService();
        ReflectionTestUtils.setField(service, "playerPackService", mock(PlayerPackService.class));
        SimPlayerContext ctx = mock(SimPlayerContext.class);
        SimBaseData baseData = mock(SimBaseData.class);
        SimCasinoData casino = mock(SimCasinoData.class);
        when(ctx.getSimBaseData()).thenReturn(baseData);
        when(baseData.getAllLevel()).thenReturn(99);
        when(casino.getCasinoId()).thenReturn(1);

        BuildingAreaTableCfg level13 = researchConfig(1106, 13);
        BuildingAreaTableCfg level23 = researchConfig(1104, 23);
        BuildingAreaTableCfg level27 = researchConfig(1103, 27);
        when(casino.findBuilding(1106)).thenReturn(mock(BuildingData.class));

        List<OperationResearchBuilding> result;
        try (MockedStatic<GameDataManager> configs = mockStatic(GameDataManager.class)) {
            // 故意按错误顺序返回，验证服务端仍按CasinoLevel排序。
            configs.when(GameDataManager::getBuildingAreaTableCfgList)
                    .thenReturn(List.of(level27, level13, level23));
            result = ReflectionTestUtils.invokeMethod(service, "buildResearchBuildings", ctx, casino);
        }

        assertEquals(List.of(1106, 1104, 1103), result.stream().map(data -> data.buildingId).toList());
        assertEquals(List.of(1, 2, 3), result.stream().map(data -> data.state).toList());
    }

    /** 完整看板和每秒刷新均限制展示人数，覆盖超限、满员、未满员和零容量。 */
    @ParameterizedTest
    @CsvSource({"210, 6, 6, true", "6, 6, 6, true", "3, 6, 3, false", "0, 6, 0, false",
            "210, 0, 0, false"})
    void shouldCapDisplayedCapacityInBothResponses(int count, int capacity, int expected,
                                                    boolean expectedOverloaded) {
        SimOperationDashboardService service = new SimOperationDashboardService();
        SimBuildingService buildingService = mock(SimBuildingService.class);
        SimConfigCacheService configCache = mock(SimConfigCacheService.class);
        ReflectionTestUtils.setField(service, "buildingService", buildingService);
        ReflectionTestUtils.setField(service, "configCache", configCache);

        SimPlayerContext ctx = mock(SimPlayerContext.class);
        SimCasinoData casino = mock(SimCasinoData.class);
        BuildingData building = mock(BuildingData.class);
        BuildingAreaTableCfg areaCfg = mock(BuildingAreaTableCfg.class);
        BuildingUpgradeTableCfg levelCfg = mock(BuildingUpgradeTableCfg.class);
        when(ctx.getCurrentCasino()).thenReturn(casino);
        when(casino.getCasinoId()).thenReturn(1);
        when(casino.getBuildingData()).thenReturn(Map.of(101, building));
        when(casino.findBuilding(101)).thenReturn(building);
        when(casino.countGenerateInWindow(anyLong(), anyLong())).thenReturn(count);
        when(casino.countBuildingInteractionsInWindow(eq(101), anyLong(), anyLong())).thenReturn(count);
        when(building.getId()).thenReturn(101);
        when(building.getLevel()).thenReturn(1);
        when(areaCfg.getId()).thenReturn(101);
        when(areaCfg.getRegionID()).thenReturn(1);
        when(areaCfg.getType()).thenReturn(BuildingType.REST.getCode());
        when(levelCfg.getMaxInteractionCount()).thenReturn(capacity);
        when(configCache.getBuildingUpgradeCfg(101, 1)).thenReturn(levelCfg);
        when(buildingService.computePerMinuteOutput(ctx, casino)).thenReturn(Map.of());
        when(buildingService.computeDashboardBuildingValues(ctx, building)).thenReturn(Map.of());

        try (MockedStatic<GameDataManager> configs = mockStatic(GameDataManager.class)) {
            configs.when(GameDataManager::getBuildingAreaTableCfgList).thenReturn(List.of(areaCfg));
            configs.when(() -> GameDataManager.getBuildingAreaTableCfg(101)).thenReturn(areaCfg);
            service.onDashboard(ctx);
            service.onCapacity(ctx);
        }

        ArgumentCaptor<Object> messages = ArgumentCaptor.forClass(Object.class);
        verify(ctx, times(2)).send(messages.capture());
        ResOperationDashboard dashboard = (ResOperationDashboard) messages.getAllValues().get(0);
        ResOperationCapacity refresh = (ResOperationCapacity) messages.getAllValues().get(1);
        assertEquals(Code.SUCCESS, dashboard.code);
        assertEquals(Code.SUCCESS, refresh.code);
        assertEquals(expected, dashboard.overview.currentCapacity);
        assertEquals(capacity, dashboard.overview.totalCapacity);
        assertEquals(expectedOverloaded, dashboard.overview.capacityOverloaded);
        assertEquals(1, dashboard.buildings.size());
        assertEquals(expected, dashboard.buildings.get(0).currentCapacity);
        assertEquals(capacity, dashboard.buildings.get(0).capacity);
        assertEquals(expectedOverloaded, dashboard.buildings.get(0).capacityOverloaded);
        assertEquals(expected, refresh.currentCapacity);
        assertEquals(capacity, refresh.totalCapacity);
        assertEquals(expectedOverloaded, refresh.capacityOverloaded);
        assertEquals(1, refresh.buildings.size());
        assertEquals(expected, refresh.buildings.get(0).currentCapacity);
        assertEquals(capacity, refresh.buildings.get(0).capacity);
        assertEquals(expectedOverloaded, refresh.buildings.get(0).capacityOverloaded);
    }

    private BuildingAreaTableCfg researchConfig(int id, int casinoLevel) {
        BuildingAreaTableCfg cfg = mock(BuildingAreaTableCfg.class);
        when(cfg.getId()).thenReturn(id);
        when(cfg.getRegionID()).thenReturn(1);
        when(cfg.getType()).thenReturn(BuildingType.GAME.getCode());
        when(cfg.getCasinoLevel()).thenReturn(casinoLevel);
        return cfg;
    }
}
