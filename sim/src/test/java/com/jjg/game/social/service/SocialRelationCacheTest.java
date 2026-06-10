package com.jjg.game.social.service;

import com.jjg.game.social.dao.FriendDao;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SocialRelationCacheTest {
    private static final String BLACKLIST_KEY = "social:blacklist:1001";
    private static final String INVALIDATE_CHANNEL = "social:blacklist:invalidate";

    @Test
    void loadsBlacklistFromMongoOnLocalCacheMiss() {
        TestContext ctx = context();
        when(ctx.friendDao.getBlacklistIds(1001L)).thenReturn(Set.of(2001L, 2002L));

        assertTrue(ctx.cache.isBlacklisted(1001L, 2001L));
        assertTrue(ctx.cache.isBlacklisted(1001L, 2002L));
        assertFalse(ctx.cache.isBlacklisted(1001L, 2003L));

        verify(ctx.friendDao, times(1)).getBlacklistIds(1001L);
        verify(ctx.redis, never()).opsForSet();
        verify(ctx.redis, never()).delete(BLACKLIST_KEY);
    }

    @Test
    void usesLocalCacheAfterMongoLoad() {
        TestContext ctx = context();
        when(ctx.friendDao.getBlacklistIds(1001L)).thenReturn(Set.of(2001L));

        assertTrue(ctx.cache.isBlacklisted(1001L, 2001L));
        assertTrue(ctx.cache.isBlacklisted(1001L, 2001L));

        verify(ctx.friendDao, times(1)).getBlacklistIds(1001L);
        verify(ctx.redis, never()).opsForSet();
    }

    @Test
    void onlyPublishesInvalidationOnBlacklistAdd() {
        TestContext ctx = context();

        ctx.cache.publishInvalidate(1001L);

        verify(ctx.redis).convertAndSend(INVALIDATE_CHANNEL, "1001");
        verify(ctx.redis, never()).opsForSet();
        verify(ctx.redis, never()).delete(BLACKLIST_KEY);
    }

    @Test
    void onlyPublishesInvalidationOnBlacklistRemove() {
        TestContext ctx = context();

        ctx.cache.publishInvalidate(1001L);

        verify(ctx.redis).convertAndSend(INVALIDATE_CHANNEL, "1001");
        verify(ctx.redis, never()).opsForSet();
        verify(ctx.redis, never()).delete(BLACKLIST_KEY);
    }

    @Test
    void onlyPublishesInvalidationOnBlacklistClear() {
        TestContext ctx = context();

        ctx.cache.publishInvalidate(1001L);

        verify(ctx.redis).convertAndSend(INVALIDATE_CHANNEL, "1001");
        verify(ctx.redis, never()).opsForSet();
        verify(ctx.redis, never()).delete(BLACKLIST_KEY);
    }

    private static TestContext context() {
        SocialRelationCache cache = new SocialRelationCache();
        FriendDao friendDao = mock(FriendDao.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ReflectionTestUtils.setField(cache, "friendDao", friendDao);
        ReflectionTestUtils.setField(cache, "stringRedisTemplate", redis);
        return new TestContext(cache, friendDao, redis);
    }

    private record TestContext(SocialRelationCache cache,
                               FriendDao friendDao,
                               StringRedisTemplate redis) {
    }
}
