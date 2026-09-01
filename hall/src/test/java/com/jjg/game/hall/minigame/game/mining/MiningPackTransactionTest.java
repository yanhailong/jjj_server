package com.jjg.game.hall.minigame.game.mining;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.task.manager.TaskManager;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MiningPackTransactionTest {
    private PlayerPackService packs;
    private Player player;
    private CoreLogger logger;
    private final AtomicReference<String> persisted = new AtomicReference<>();

    @BeforeAll static void fixtures() throws Exception { MiningFixtures.install(); }
    @BeforeEach @SuppressWarnings({"unchecked", "rawtypes"}) void setup() throws Exception {
        player = new Player(); player.setId(123);
        PlayerPack initial = new PlayerPack(123); initial.addItem(1024034, 2, 999999); initial.setMiningState("old");
        persisted.set(JSON.toJSONString(initial));
        packs = spy(new PlayerPackService());
        doAnswer(i -> JSON.parseObject(persisted.get(), PlayerPack.class)).when(packs).getFromAllDB(123);
        RedisTemplate template = mock(RedisTemplate.class); HashOperations hash = mock(HashOperations.class);
        when(template.opsForHash()).thenReturn(hash);
        doAnswer(i -> { persisted.set(JSON.toJSONString(i.getArgument(2))); return null; }).when(hash).put(eq("playerPack"), eq(123L), any());
        ReflectionTestUtils.setField(packs, "redisTemplate", template);
        RedisLock redisLock = mock(RedisLock.class); ReentrantLock localLock = new ReentrantLock();
        when(redisLock.tryLockWithDefaultTime(anyString())).thenAnswer(i -> localLock.tryLock(500, TimeUnit.MILLISECONDS));
        doAnswer(i -> { localLock.unlock(); return null; }).when(redisLock).tryUnlock(anyString());
        ReflectionTestUtils.setField(packs, "redisLock", redisLock);
        logger = mock(CoreLogger.class); ReflectionTestUtils.setField(packs, "coreLogger", logger);
        ReflectionTestUtils.setField(packs, "taskManager", mock(TaskManager.class));
    }

    private PlayerPack stored() { return JSON.parseObject(persisted.get(), PlayerPack.class); }

    @Test void stateAndItemExchangeCommitTogetherAndStaleRequestIsReadOnly() {
        assertEquals(Code.SUCCESS, packs.exchangeMiningItems(player, Map.of(1024034, 1L), Map.of(1024037, 1L),
                AddType.MINING_DIG, "test", "old", "new").code);
        assertEquals("new", stored().getMiningState()); assertEquals(1, stored().getItemCount(1024034));
        assertEquals(1, stored().getItemCount(1024037)); String committed = persisted.get();
        assertEquals(Code.REPEAT_OP, packs.exchangeMiningItems(player, Map.of(1024034, 1L), Map.of(1024037, 1L),
                AddType.MINING_DIG, "test", "old", "new").code);
        assertEquals(committed, persisted.get());
    }

    @Test void metadataOnlyClaimStillPersistsAndInsufficientItemsDoNotAdvanceVersion() {
        String before = persisted.get();
        assertEquals(Code.NOT_ENOUGH_ITEM, packs.exchangeMiningItems(player, Map.of(1024034, 3L), Map.of(),
                AddType.MINING_DIG, "test", "old", "new").code);
        assertEquals(before, persisted.get());
        assertEquals(Code.SUCCESS, packs.exchangeMiningItems(player, Map.of(), Map.of(),
                AddType.MINING_DIG, "test", "old", "new").code);
        assertEquals("new", stored().getMiningState());
    }

    @Test void loggingFailureAfterCommitDoesNotTurnSuccessIntoRetryableFailure() {
        doThrow(new IllegalStateException("log sink unavailable")).when(logger)
                .consumeItem(anyLong(), anyMap(), anyMap(), anyMap(), any());
        assertEquals(Code.SUCCESS, packs.exchangeMiningItems(player, Map.of(1024034, 1L), Map.of(),
                AddType.MINING_DIG, "test", "old", "new").code);
        assertEquals("new", stored().getMiningState()); assertEquals(1, stored().getItemCount(1024034));
    }

    @Test void concurrentRequestsWithSameVersionOnlyCommitOneReward() throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Integer> operation = () -> { start.await(); return packs.exchangeMiningItems(player,
                    Map.of(1024034, 1L), Map.of(1024037, 1L), AddType.MINING_DIG, "race", "old", "new").code; };
            Future<Integer> one = executor.submit(operation), two = executor.submit(operation); start.countDown();
            assertEquals(java.util.Set.of(Code.SUCCESS, Code.REPEAT_OP), java.util.Set.of(one.get(5, TimeUnit.SECONDS), two.get(5, TimeUnit.SECONDS)));
        }
        assertEquals(1, stored().getItemCount(1024034)); assertEquals(1, stored().getItemCount(1024037));
    }
}
