package com.jjg.game.reddot;

import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.pb.reddot.NotifyRedDot;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.jjg.game.core.pb.reddot.RedDotDetails.RedDotModule.*;
import static com.jjg.game.core.pb.reddot.RedDotDetails.RedDotType.*;

class RedDotCoreTest {
    @Test void readRequestAndNewEnumRoundTripThroughProductionSerializer() {
        var request = new com.jjg.game.core.pb.reddot.ReqMarkRedDotRead();
        request.module = EMPLOYEE; request.submodule = 4; request.entityIds = List.of(101, 102);
        byte[] bytes = com.jjg.game.common.protostuff.ProtostuffUtil.serialize(request);
        var decoded = com.jjg.game.common.protostuff.ProtostuffUtil.deserialize(bytes, request.getClass());
        assertEquals(EMPLOYEE, decoded.module); assertEquals(4, decoded.submodule); assertEquals(request.entityIds, decoded.entityIds);
        var dot = new RedDotManager(null, null, null).buildRedDotDetails(BUILDING, 1, 3, COUNT);
        var decodedDot = com.jjg.game.common.protostuff.ProtostuffUtil.deserialize(
                com.jjg.game.common.protostuff.ProtostuffUtil.serialize(dot), RedDotDetails.class);
        assertEquals(BUILDING, decodedDot.getRedDotModule()); assertEquals(3, decodedDot.getCount());
    }
    @Test void numericTypesPreserveQuantityAndClampNegative() {
        RedDotManager manager = new RedDotManager(null, null, null);
        assertEquals(3, manager.buildRedDotDetails(ACTIVITY, 25, 3, COUNT).getCount());
        assertEquals(0, manager.buildRedDotDetails(ACTIVITY, 25, -3, COUNT).getCount());
        assertEquals(1, manager.buildRedDotDetails(ACTIVITY, 14, 3, COMMON).getCount());
        assertEquals(1, manager.buildRedDotDetails(BUILDING, 1, 3, EXCLAMATION).getCount());
        assertEquals(8, VISIT.ordinal());
        assertEquals(14, BUILDING.ordinal());
    }

    @Test void readAcknowledgementCannotClearActionableCounts() {
        RedDotManager manager = new RedDotManager(null, null, null);
        IRedDotService service = mock(IRedDotService.class);
        PlayerController player = mock(PlayerController.class);
        when(player.playerId()).thenReturn(1L);
        manager.registerService(EMPLOYEE, 3, service);
        manager.markRead(player, EMPLOYEE, 3, List.of(10));
        verify(service).markRead(1L, 3, List.of(10));
        verify(service, never()).initialize(anyLong(), anyInt());
        manager.markRead(player, EMPLOYEE, 0, List.of());
        verify(service, times(1)).markRead(anyLong(), anyInt(), anyList());
    }

    @Test void readAcknowledgementReturnsCommonNotifySnapshot() {
        RedDotManager manager = new RedDotManager(null, null, null);
        IRedDotService service = mock(IRedDotService.class);
        PlayerController player = mock(PlayerController.class);
        when(player.playerId()).thenReturn(1L);
        when(service.markRead(1L, 4, List.of(10))).thenReturn(true);
        when(service.initialize(1L, 4)).thenReturn(List.of(manager.buildRedDotDetails(EMPLOYEE, 4, 0)));
        manager.registerService(EMPLOYEE, 4, service);
        manager.markRead(player, EMPLOYEE, 4, List.of(10));
        verify(player).send(any(NotifyRedDot.class));
    }
}
