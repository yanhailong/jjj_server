package com.jjg.game.sim.service;

import com.jjg.game.core.constant.*;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SimMiningRewardServiceTest {
    private MockedStatic<GameDataManager> tables;
    private SimMiningRewardService service;
    private SimPlayerContext ctx;
    private SimConfigCacheService configs;
    private PlayerPackService shards;
    private SimPackService special;
    private PlayerPackService entry;
    private VisitorQuestCfg guestCfg;

    @BeforeEach void setup() {
        tables = mockStatic(GameDataManager.class);
        ctx = new SimPlayerContext(); ctx.setPlayerId(10001L);
        SimCasinoData casino = new SimCasinoData(); casino.setPlayerId(10001L); casino.setCasinoId(1);
        ctx.setCurrentCasino(casino);
        SimPlayerContextRegistry contexts = new SimPlayerContextRegistry(); contexts.putContext(ctx);
        configs = mock(SimConfigCacheService.class);
        shards = mock(PlayerPackService.class);
        when(shards.addItems(anyLong(), anyMap(), any(), anyString(), eq(true)))
                .thenReturn(new CommonResult<>(Code.SUCCESS, new ItemOperationResult()));
        guestCfg = mock(VisitorQuestCfg.class);
        when(guestCfg.getId()).thenReturn(6106);
        when(guestCfg.getDuplicatetoShard()).thenReturn(List.of(1025506, 1025606, 30));
        when(configs.getVisitorQuestCfgByItemId(1025506)).thenReturn(guestCfg);
        tables.when(() -> GameDataManager.getVisitorQuestCfg(6106)).thenReturn(guestCfg);
        EmployeeProfileCfg employeeCfg = mock(EmployeeProfileCfg.class);
        when(employeeCfg.getId()).thenReturn(7101);
        when(employeeCfg.getDuplicatetoShard()).thenReturn(List.of(1027401, 1027501, 20));
        when(configs.getEmployeeProfileCfgByItemId(1027401)).thenReturn(employeeCfg);
        tables.when(() -> GameDataManager.getEmployeeProfileCfg(7101)).thenReturn(employeeCfg);
        installItem(1025506, GameConstant.Item.ITEM_TYPE_SIM_GUEST);
        installItem(1027401, GameConstant.Item.ITEM_TYPE_SIM_EMPLOYEE);
        SimGuestService guests = new SimGuestService();
        SimEmployeeService employees = new SimEmployeeService();
        for (Object handler : List.of(guests, employees)) {
            ReflectionTestUtils.setField(handler, "configCache", configs);
            ReflectionTestUtils.setField(handler, "playerPackService", shards);
            ReflectionTestUtils.setField(handler, "employeeRedDotService", mock(SimEmployeeRedDotService.class));
            ReflectionTestUtils.setField(handler, "simTaskService", mock(SimTaskService.class));
        }
        special = spy(new SimPackService());
        ReflectionTestUtils.setField(special, "simPlayerContextRegistry", contexts);
        ReflectionTestUtils.setField(special, "simGuestService", guests);
        ReflectionTestUtils.setField(special, "simEmployeeService", employees);
        // 不模拟角色发放，只略过节点路由和日志余额查询，实际执行 SIM 入账与角色业务方法。
        doAnswer(inv -> special.addItemsHere(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)))
                .when(special).addItems(anyLong(), anyList(), any(), anyString(), anyBoolean());
        entry = new PlayerPackService();
        ReflectionTestUtils.setField(entry, "specialItemListener", special);
        service = new SimMiningRewardService(contexts, configs, entry);
    }

    private void installItem(int id, int type) {
        ItemCfg cfg = mock(ItemCfg.class); when(cfg.getId()).thenReturn(id); when(cfg.getItemType()).thenReturn(type);
        when(cfg.getIsBag()).thenReturn(false);
        tables.when(() -> GameDataManager.getItemCfg(id)).thenReturn(cfg);
    }

    @AfterEach void close() { tables.close(); }

    @Test void queenUnlocksThroughSamePackAndGuestFlowAsAllianceShop() {
        assertEquals(Code.SUCCESS, service.grant(10001L, Map.of(1025506, 1L), AddType.MINING_EXCHANGE, "queen-order").code);
        GuestData unlocked = ctx.getCurrentCasino().findGuestData(6106);
        assertNotNull(unlocked); assertEquals(1, unlocked.getLevel()); assertEquals(1, unlocked.getStar());
        verify(shards, never()).addItems(anyLong(), anyMap(), any(), anyString(), anyBoolean());
        assertEquals(Code.SUCCESS, service.grant(10001L, Map.of(1025506, 2L), AddType.MINING_EXCHANGE, "queen-repeat").code);
        verify(shards).addItems(eq(10001L), eq(Map.of(1025606, 60L)), eq(AddType.MINING_EXCHANGE), anyString(), eq(true));
        assertEquals(1, ctx.getCurrentCasino().getGuestMap().size());
    }

    @Test void employeeUnlocksAndExtraCopiesBecomeConfiguredShards() {
        assertEquals(Code.SUCCESS, service.grant(10001L, Map.of(1027401, 3L), AddType.MINING_EXCHANGE, "employee-order").code);
        assertNotNull(ctx.getEmployee(7101)); assertEquals(1, ctx.getEmployee(7101).getLevel());
        verify(shards).addItems(eq(10001L), eq(Map.of(1027501, 40L)), eq(AddType.MINING_EXCHANGE), anyString(), eq(true));
    }

    @Test void nodeWithoutSimHandlerMustNotSilentlySucceed() {
        assertEquals(Code.FAIL, new PlayerPackService().addItems(10001L, Map.of(1025506, 1L), AddType.MINING_EXCHANGE).code);
        assertNull(ctx.getCurrentCasino().findGuestData(6106));
    }

    @Test void missingContextOrConfigurationIsRejectedBeforeGrant() {
        assertEquals(Code.NOT_FOUND, service.grant(999L, Map.of(1025506, 1L), AddType.MINING_EXCHANGE, "order").code);
        when(configs.getVisitorQuestCfgByItemId(1025506)).thenReturn(null);
        assertEquals(Code.SAMPLE_ERROR, service.grant(10001L, Map.of(1025506, 1L), AddType.MINING_EXCHANGE, "order").code);
        verify(special, never()).addItems(anyLong(), anyList(), any(), anyString(), anyBoolean());
    }

    @Test void failedDuplicateShardGrantIsNotReportedAsSuccess() {
        when(shards.addItems(anyLong(), anyMap(), any(), anyString(), eq(true))).thenReturn(new CommonResult<>(Code.FAIL));
        assertEquals(Code.EXCEPTION, service.grant(10001L, Map.of(1027401, 2L), AddType.MINING_EXCHANGE, "failure").code);
        assertNull(ctx.getEmployee(7101));
    }
}
