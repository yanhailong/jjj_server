package com.jjg.game.sim.service;

import com.jjg.game.core.dao.RedDotReadDao;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimEmployeeRedDotTest {
    @Test void upgradeAndStarOnSameEmployeeOnlyCountOnce() {
        SimEmployeeRedDotService service = new SimEmployeeRedDotService();
        SimPlayerContextRegistry contexts = mock(SimPlayerContextRegistry.class);
        SimPlayerContext ctx = mock(SimPlayerContext.class);
        SimEmployeeData e1 = new SimEmployeeData(), e2 = new SimEmployeeData();
        e1.setEmployeeId(11); e1.setStar(1); e1.setLevel(1);
        e2.setEmployeeId(12); e2.setStar(1); e2.setLevel(1);
        when(contexts.getContext(1)).thenReturn(ctx);
        when(ctx.getEmployeeMap()).thenReturn(Map.of(11, e1, 12, e2));
        Player player = mock(Player.class);
        CorePlayerService players = mock(CorePlayerService.class); when(players.get(1)).thenReturn(player);
        PlayerPackService packs = mock(PlayerPackService.class);
        when(packs.findSatisfiedItemRequirements(eq(player), anyList())).thenAnswer(call -> {
            List<?> costs = call.getArgument(1);
            return new HashSet<>(java.util.stream.IntStream.range(0, costs.size()).boxed().toList());
        });
        SimConfigCacheService configs = mock(SimConfigCacheService.class);
        EmployeeStarCfg star = mock(EmployeeStarCfg.class);
        when(star.getStarUpCost()).thenReturn(10); when(star.getLevelCap()).thenReturn(10);
        when(configs.getEmployeeStarCfgMap()).thenReturn(Map.of(11, Map.of(1, star, 2, star), 12, Map.of(1, star, 2, star)));
        EmployeeLevelCfg level = mock(EmployeeLevelCfg.class); when(level.getUpgradeCost()).thenReturn(Map.of(100, 10L));
        when(configs.getEmployeeLevelCfgMap()).thenReturn(Map.of(11, Map.of(2, level), 12, Map.of(2, level)));
        ReflectionTestUtils.setField(service, "contextRegistry", contexts);
        ReflectionTestUtils.setField(service, "configCache", configs);
        ReflectionTestUtils.setField(service, "corePlayerService", players);
        ReflectionTestUtils.setField(service, "playerPackService", packs);
        ReflectionTestUtils.setField(service, "redDotManager", new RedDotManager(null, null, null));
        EmployeeProfileCfg profile = mock(EmployeeProfileCfg.class); when(profile.getDuplicatetoShard()).thenReturn(List.of(0, 100, 1));
        try (var staticConfigs = mockStatic(GameDataManager.class)) {
            staticConfigs.when(() -> GameDataManager.getEmployeeProfileCfg(anyInt())).thenReturn(profile);
            var dot = service.initialize(1, 3).getFirst();
            assertEquals(2, dot.getCount());
            assertTrue(dot.getExtra().contains("levelUpIds")); assertTrue(dot.getExtra().contains("starUpIds"));
            e1.setLevel(10); e2.setLevel(10);
            when(star.getStarUpCost()).thenReturn(0);
            assertEquals(0, service.initialize(1, 3).getFirst().getCount());
        }
    }

    @Test void readingNewEmployeesDoesNotClearGrowthCounts() {
        SimEmployeeRedDotService service = new SimEmployeeRedDotService();
        RedDotReadDao reads = mock(RedDotReadDao.class);
        SimPlayerContextRegistry contexts = mock(SimPlayerContextRegistry.class);
        ReflectionTestUtils.setField(service, "redDotReadDao", reads);
        ReflectionTestUtils.setField(service, "contextRegistry", contexts);
        assertFalse(service.markRead(1, 3, List.of(11)));
        assertTrue(service.markRead(1, 4, List.of(11)));
        verify(reads).read(1, "newEmployee", List.of(11));
        verifyNoMoreInteractions(reads);
    }
}
