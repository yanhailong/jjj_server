package com.jjg.game.core.service;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.core.base.condition.ConditionParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class GameFunctionServiceTest {

    @Test
    void doesNotParseFunctionConditionsOnGameNode() {
        ConditionParser conditionParser = mock(ConditionParser.class);
        NodeConfig nodeConfig = new NodeConfig();
        nodeConfig.setType(NodeType.GAME.name());
        GameFunctionService service = new GameFunctionService(null, conditionParser, null, nodeConfig);

        assertTrue(service.needMonitorEvents().isEmpty());
        verifyNoInteractions(conditionParser);
    }
}
