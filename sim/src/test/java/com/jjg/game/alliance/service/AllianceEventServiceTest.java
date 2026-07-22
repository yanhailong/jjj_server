package com.jjg.game.alliance.service;

import com.jjg.game.core.base.condition.numeric.ConditionEvent;
import com.jjg.game.sim.service.SimTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AllianceEventServiceTest {

    @Test
    void legacyAllianceProtocolDoesNotFanOutToSimTasks() {
        AllianceTaskService allianceTasks = mock(AllianceTaskService.class);
        SimTaskService simTasks = mock(SimTaskService.class);
        AllianceEventService service = new AllianceEventService();
        ReflectionTestUtils.setField(service, "taskService", allianceTasks);
        ReflectionTestUtils.setField(service, "simTaskService", simTasks);

        service.onEvent(7L, 12303, 9, 2);

        verify(allianceTasks).onConditionEvent(eq(7L), any(ConditionEvent.class));
        verifyNoInteractions(simTasks);
    }
}
