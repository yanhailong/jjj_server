package com.jjg.game.ploy.games.mining;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MiningAdTicketTest {
    @Test @SuppressWarnings({"unchecked", "rawtypes"}) void verifiedTransactionIssuesOneTicketBoundToPlayerAndDayWithoutRenewal() {
        RedissonClient redis = mock(RedissonClient.class);
        Map<String, String> storage = new HashMap<>(); Map<String, RBucket> buckets = new HashMap<>();
        when(redis.getBucket(anyString(), eq(StringCodec.INSTANCE))).thenAnswer(i -> {
            String key = i.getArgument(0);
            return buckets.computeIfAbsent(key, ignored -> {
                RBucket<String> bucket = mock(RBucket.class);
                when(bucket.get()).thenAnswer(x -> storage.get(key));
                when(bucket.setIfAbsent(anyString(), any(Duration.class))).thenAnswer(x -> storage.putIfAbsent(key, x.getArgument(0)) == null);
                doAnswer(x -> { storage.put(key, x.getArgument(0)); return null; }).when(bucket).set(anyString(), any(Duration.class));
                return bucket;
            });
        });
        MiningAdTicketService ads = new MiningAdTicketService(redis, new MiningConfig());
        String ticket = ads.issueVerifiedTicket(1, "provider:transaction:abc", 20260831);
        assertTrue(ads.valid(1, 20260831, ticket)); assertFalse(ads.valid(2, 20260831, ticket)); assertFalse(ads.valid(1, 20260901, ticket));
        assertEquals(ticket, ads.issueVerifiedTicket(1, "provider:transaction:abc", 20260831));
        assertThrows(MiningException.class, () -> ads.issueVerifiedTicket(2, "provider:transaction:abc", 20260831));
        storage.remove("mining:ad:ticket:" + ticket);
        assertEquals(ticket, ads.issueVerifiedTicket(1, "provider:transaction:abc", 20260831));
        assertFalse(ads.valid(1, 20260831, ticket));
        verify(buckets.get("mining:ad:ticket:" + ticket), times(1)).set(eq("1:20260831"), eq(Duration.ofSeconds(600)));
    }
}
