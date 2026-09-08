package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.dao.RedDotReadDao;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.VisitorBondsCfg;
import com.jjg.game.sampledata.bean.VisitorQuestCfg;
import com.jjg.game.sampledata.bean.VisitorStarCfg;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 回归游客页签的升星/羁绊去重计数与已读刷新。 */
class SimVisitorEntryRedDotTest {
    @Test
    void combinesStarsAndOverlappingBondsAndRefreshesAfterReading() {
        SimEmployeeRedDotService service = new SimEmployeeRedDotService();
        SimPlayerContextRegistry registry = mock(SimPlayerContextRegistry.class);
        SimPlayerContext ctx = mock(SimPlayerContext.class);
        SimCasinoData casino = mock(SimCasinoData.class);
        CorePlayerService players = mock(CorePlayerService.class);
        Player player = mock(Player.class);
        PlayerPackService pack = mock(PlayerPackService.class);
        SimConfigCacheService configs = mock(SimConfigCacheService.class);
        RedDotReadDao unread = mock(RedDotReadDao.class);
        RedDotManager manager = mock(RedDotManager.class);
        ReflectionTestUtils.setField(service, "contextRegistry", registry);
        ReflectionTestUtils.setField(service, "corePlayerService", players);
        ReflectionTestUtils.setField(service, "playerPackService", pack);
        ReflectionTestUtils.setField(service, "configCache", configs);
        ReflectionTestUtils.setField(service, "redDotReadDao", unread);
        ReflectionTestUtils.setField(service, "redDotManager", manager);
        when(registry.getContext(1L)).thenReturn(ctx);
        when(ctx.getCurrentCasino()).thenReturn(casino);
        when(casino.getCasinoId()).thenReturn(1);
        when(players.get(1L)).thenReturn(player);
        Map<Integer, GuestData> guests = Map.of(101, guest(101), 102, guest(102), 103, guest(103));
        when(casino.getGuestMap()).thenReturn(guests);
        when(manager.buildRedDotDetails(any(), eq(6), anyLong(), any())).thenAnswer(call -> {
            RedDotDetails dot = new RedDotDetails();
            dot.setCount(call.getArgument(2));
            dot.setRedDotType(call.getArgument(3));
            return dot;
        });

        try (MockedStatic<GameDataManager> data = mockStatic(GameDataManager.class)) {
            VisitorStarCfg star = mock(VisitorStarCfg.class);
            VisitorQuestCfg visitor = mock(VisitorQuestCfg.class);
            when(star.getAscend()).thenReturn(10);
            when(visitor.getDuplicatetoShard()).thenReturn(List.of(1001, 2001, 1));
            // 仅101有下一星级；102、103仍可能因新羁绊亮红点。
            when(configs.getVisitorStarCfgByGuest(101, 1)).thenReturn(star);
            when(configs.getVisitorStarCfgByGuest(101, 2)).thenReturn(star);
            data.when(() -> GameDataManager.getVisitorQuestCfg(101)).thenReturn(visitor);
            VisitorBondsCfg first = mock(VisitorBondsCfg.class);
            VisitorBondsCfg second = mock(VisitorBondsCfg.class);
            when(first.getMembers()).thenReturn(List.of(101, 102, 999));
            when(second.getMembers()).thenReturn(List.of(102, 103));
            data.when(() -> GameDataManager.getVisitorBondsCfg(301)).thenReturn(first);
            data.when(() -> GameDataManager.getVisitorBondsCfg(302)).thenReturn(second);
            when(unread.unread(1L, "newBond:1")).thenReturn(Set.of(301, 302));
            when(pack.findSatisfiedItemRequirements(eq(player), anyList())).thenReturn(Set.of(0));
            assertEntry(service, 3, List.of(101, 102, 103));

            // 两个羁绊均已查看后，只保留仍可升星的101。
            doAnswer(call -> {
                when(unread.unread(1L, "newBond:1")).thenReturn(Set.of());
                return null;
            }).when(unread).read(eq(1L), eq("newBond:1"), anyCollection());
            service.markRead(1L, 5, List.of(301, 302));
            verify(manager).updateRedDotByInitialize(RedDotDetails.RedDotModule.EMPLOYEE, List.of(6), 1L);
            assertEntry(service, 1, List.of(101));

            when(pack.findSatisfiedItemRequirements(eq(player), anyList())).thenReturn(Set.of());
            assertEntry(service, 0, List.of());

            // 只有新羁绊也显示成员人数，并排除未拥有的999。
            when(unread.unread(1L, "newBond:1")).thenReturn(Set.of(301));
            assertEntry(service, 2, List.of(101, 102));
        }
    }

    private GuestData guest(int id) {
        GuestData guest = mock(GuestData.class);
        when(guest.getId()).thenReturn(id);
        when(guest.getStar()).thenReturn(1);
        return guest;
    }

    private void assertEntry(SimEmployeeRedDotService service, long count, List<Integer> ids) {
        RedDotDetails dot = service.initialize(1L, 6).getFirst();
        assertEquals(count, dot.getCount());
        assertEquals(RedDotDetails.RedDotType.COUNT, dot.getRedDotType());
        assertEquals(ids, JSON.parseObject(dot.getExtra()).getJSONArray("ids").toJavaList(Integer.class));
    }
}
