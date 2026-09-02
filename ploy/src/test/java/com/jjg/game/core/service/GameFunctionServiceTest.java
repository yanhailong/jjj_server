package com.jjg.game.core.service;

import com.jjg.game.core.base.condition.ConditionParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class GameFunctionServiceTest {

    @Test
    void doesNotParseFunctionConditionsWithoutNotificationListener() {
        ConditionParser conditionParser = mock(ConditionParser.class);
        GameFunctionService service = new GameFunctionService(null, conditionParser, null, List.of());

        assertTrue(service.needMonitorEvents().isEmpty());
        verifyNoInteractions(conditionParser);
    }
}
