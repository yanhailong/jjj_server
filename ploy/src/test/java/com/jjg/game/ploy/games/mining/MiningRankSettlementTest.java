package com.jjg.game.ploy.games.mining;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerPackService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MiningRankSettlementTest {
    @Test void rankResponseIncludesStoredFrameAndCurrentSelfFrame() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        MiningRankService.Entry entry = new MiningRankService.Entry();
        entry.playerId = 11; entry.seasonId = "s1"; entry.depth = 10; entry.headFrameId = 2002;
        when(mongo.find(any(Query.class), eq(MiningRankService.Entry.class))).thenReturn(List.of(entry));

        Player player = new Player(); player.setId(22); player.setHeadFrameId(2003);
        MiningConfig.Season season = new MiningConfig.Season(); season.id = "s1"; season.rewards = List.of();
        MiningRankService service = new MiningRankService(mongo, mock(RedissonClient.class),
                mock(PlayerPackService.class), mock(MailService.class), new MiningConfig());

        var response = service.rank(player, season);
        assertEquals(2002, response.ranks.getFirst().headFrameId);
        assertEquals(2003, response.self.headFrameId);
    }

    @Test void mailFailureRetriesFrozenAwardsAndCompletedSeasonIsNotPaidAgain() {
        MongoTemplate mongo = mock(MongoTemplate.class); RedissonClient redis = mock(RedissonClient.class);
        PlayerPackService packs = mock(PlayerPackService.class); MailService mail = mock(MailService.class);
        RReadWriteLock rw = mock(RReadWriteLock.class); RLock lock = mock(RLock.class);
        when(redis.getReadWriteLock(anyString())).thenReturn(rw); when(rw.writeLock()).thenReturn(lock); when(lock.tryLock()).thenReturn(true);
        MiningConfig cfg = new MiningConfig(); MiningConfig.Season season = new MiningConfig.Season();
        season.id = "ended"; season.startTime = 1; season.endTime = 2;
        MiningConfig.RankReward reward = new MiningConfig.RankReward(); reward.from = 1; reward.to = 300; reward.items = Map.of(1024034, 5L);
        season.rewards = List.of(reward); cfg.seasons = List.of(season);
        MiningRankService.Entry entry = new MiningRankService.Entry(); entry.playerId = 11; entry.seasonId = season.id; entry.depth = 10;
        when(mongo.stream(any(Query.class), eq(MiningRankService.Entry.class))).thenAnswer(i -> Stream.of(entry));
        when(mongo.find(any(Query.class), eq(MiningRankService.Entry.class))).thenReturn(List.of(entry));
        MiningState state = MiningFixtures.flat(1, 1001); state.seasonId = season.id; state.depth = 20;
        PlayerPack pack = new PlayerPack(11); pack.setMiningState(JSON.toJSONString(state)); when(packs.getFromAllDB(11)).thenReturn(pack);
        AtomicReference<String> stored = new AtomicReference<>();
        when(mongo.findById(season.id, MiningRankService.Settlement.class)).thenAnswer(i ->
                stored.get() == null ? null : JSON.parseObject(stored.get(), MiningRankService.Settlement.class));
        when(mongo.insert(any(MiningRankService.Settlement.class))).thenAnswer(i -> { stored.set(JSON.toJSONString(i.getArgument(0))); return i.getArgument(0); });
        when(mongo.save(any(MiningRankService.Settlement.class))).thenAnswer(i -> { stored.set(JSON.toJSONString(i.getArgument(0))); return i.getArgument(0); });
        when(mail.addMailIfAbsent(eq(11L), anyString(), anyString(), anyList(), any(), eq("mining:rank:ended:11"))).thenReturn(false, true);
        MiningRankService service = new MiningRankService(mongo, redis, packs, mail, cfg);
        service.settleEndedSeasons();
        MiningRankService.Settlement pending = JSON.parseObject(stored.get(), MiningRankService.Settlement.class);
        assertEquals(0, pending.completedAt); assertEquals(5L, pending.awards.getFirst().items.get(1024034));
        reward.items = Map.of(1024034, 999L); // 已冻结的赛季不因热改配置改变奖励。
        service.settleEndedSeasons(); service.settleEndedSeasons();
        assertTrue(JSON.parseObject(stored.get(), MiningRankService.Settlement.class).completedAt > 0);
        verify(mail, times(2)).addMailIfAbsent(eq(11L), anyString(), anyString(),
                argThat(items -> items.getFirst().getItemCount() == 5), any(), eq("mining:rank:ended:11"));
        verify(mongo, times(1)).stream(any(Query.class), eq(MiningRankService.Entry.class));
    }
}
