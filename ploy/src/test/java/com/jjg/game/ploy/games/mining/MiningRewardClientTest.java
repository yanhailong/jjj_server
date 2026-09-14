package com.jjg.game.ploy.games.mining;

import com.jjg.game.common.cluster.*;
import com.jjg.game.common.rpc.*;
import com.jjg.game.core.constant.*;
import com.jjg.game.core.data.*;
import com.jjg.game.core.rpc.MiningRewardBridge;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MiningRewardClientTest {
    private MiningRewardClient client;
    private MiningRewardBridge bridge;
    private ClusterSystem cluster;
    private HashOperations hashes;
    @BeforeEach void setup() {
        client = new MiningRewardClient(); bridge = mock(MiningRewardBridge.class); cluster = mock(ClusterSystem.class);
        RedisTemplate redis = mock(RedisTemplate.class); hashes = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        ReflectionTestUtils.setField(client, "cluster", cluster);
        ReflectionTestUtils.setField(client, "redis", redis);
        ReflectionTestUtils.setField(client, "bridge", bridge);
    }
    @AfterEach void clear() { GameRpcContext.getContext().clearRpcBuilderData(); }
    @Test void missingOwnerDoesNotDispatch() {
        assertEquals(Code.NOT_FOUND, client.grant(10L, Map.of(1025506, 1L), AddType.MINING_EXCHANGE, "id").code);
        verifyNoInteractions(bridge);
    }
    @Test void targetsOwnerAndRestoresRpcContextEvenOnTimeout() {
        ClusterClient owner = mock(ClusterClient.class);
        when(hashes.get("simnode", 10L)).thenReturn("hall-owner");
        when(cluster.getClusterByPath("hall-owner")).thenReturn(owner);
        RpcReqParameterBuilder previous = RpcReqParameterBuilder.create();
        GameRpcContext.getContext().setReqParameterBuilder(previous);
        when(bridge.grantMiningRoleReward(anyLong(), anyMap(), any(), anyString())).thenAnswer(inv -> {
            assertEquals(java.util.List.of(owner), GameRpcContext.getContext().getReqParameterBuilder().getClusterClients());
            throw new IllegalStateException("timeout");
        });
        assertThrows(IllegalStateException.class, () -> client.grant(10L, Map.of(1025506, 1L), AddType.MINING_EXCHANGE, "id"));
        assertSame(previous, GameRpcContext.getContext().getReqParameterBuilder());
    }
    @Test void unknownRemoteFailureCannotTriggerRefund() {
        when(hashes.get("simnode", 10L)).thenReturn("hall-owner");
        when(cluster.getClusterByPath("hall-owner")).thenReturn(mock(ClusterClient.class));
        when(bridge.grantMiningRoleReward(anyLong(), anyMap(), any(), anyString())).thenReturn(new CommonResult<>(Code.EXCEPTION));
        assertThrows(MiningException.class, () -> client.grant(10L, Map.of(1025506, 1L), AddType.MINING_EXCHANGE, "id"));
    }
}
