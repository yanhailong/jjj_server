package com.jjg.game.social.dao;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.social.constant.SocialConst;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 私聊未读消息索引：按消息 ID 去重，并与私聊消息使用相同的保留期。 */
@Repository
public class PrivateChatUnreadDao {
    private static final long KEEP_MILLIS = (long) SocialConst.Cfg.PRIVATE_KEEP_DAYS
            * TimeHelper.ONE_DAY_OF_MILLIS;
    private static final long KEEP_SECONDS = (long) SocialConst.Cfg.PRIVATE_KEEP_DAYS
            * TimeHelper.DAY_SECOND;

    private static final String ADD_LUA = """
            local cutoff = tonumber(ARGV[1])
            local messageTime = tonumber(ARGV[2])
            local messageId = ARGV[3]
            local keepSeconds = tonumber(ARGV[4])

            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', cutoff)
            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', cutoff)
            local readTime = tonumber(redis.call('GET', KEYS[3]) or '0')
            if messageTime > cutoff and messageTime > readTime then
                redis.call('ZADD', KEYS[1], messageTime, messageId)
                redis.call('ZADD', KEYS[2], messageTime, messageId)
            end
            if redis.call('EXISTS', KEYS[1]) == 1 then
                redis.call('EXPIRE', KEYS[1], keepSeconds)
            end
            if redis.call('EXISTS', KEYS[2]) == 1 then
                redis.call('EXPIRE', KEYS[2], keepSeconds)
            end
            return redis.call('ZCARD', KEYS[1])
            """;

    private static final String CLEAR_CONVERSATION_LUA = """
            local cutoff = tonumber(ARGV[1])
            local readTime = tonumber(ARGV[2])
            local keepSeconds = tonumber(ARGV[3])

            local previousReadTime = tonumber(redis.call('GET', KEYS[3]) or '0')
            if readTime > previousReadTime then
                redis.call('SET', KEYS[3], readTime, 'EX', keepSeconds)
            else
                redis.call('EXPIRE', KEYS[3], keepSeconds)
            end

            local ids = redis.call('ZRANGEBYSCORE', KEYS[2], '-inf', readTime)
            for offset = 1, #ids, 500 do
                local last = math.min(offset + 499, #ids)
                local batch = {}
                for index = offset, last do
                    batch[#batch + 1] = ids[index]
                end
                redis.call('ZREM', KEYS[1], unpack(batch))
            end
            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', readTime)
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', cutoff)
            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', cutoff)
            if redis.call('EXISTS', KEYS[1]) == 1 then
                redis.call('EXPIRE', KEYS[1], keepSeconds)
            end
            if redis.call('EXISTS', KEYS[2]) == 1 then
                redis.call('EXPIRE', KEYS[2], keepSeconds)
            end
            return redis.call('ZCARD', KEYS[1])
            """;

    private static final String COUNT_LUA = """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            return redis.call('ZCARD', KEYS[1])
            """;

    private final RScript script;

    public PrivateChatUnreadDao(RedissonClient redissonClient) {
        this.script = redissonClient.getScript(StringCodec.INSTANCE);
    }

    public int add(long playerId, long targetId, long messageId, long messageTime) {
        Long count = script.eval(RScript.Mode.READ_WRITE, ADD_LUA, RScript.ReturnType.INTEGER,
                List.of(allKey(playerId), conversationKey(playerId, targetId), readTimeKey(playerId, targetId)),
                String.valueOf(System.currentTimeMillis() - KEEP_MILLIS), String.valueOf(messageTime),
                String.valueOf(messageId), String.valueOf(KEEP_SECONDS));
        return toCount(count);
    }

    public int clearConversation(long playerId, long targetId, long readTime) {
        Long count = script.eval(RScript.Mode.READ_WRITE, CLEAR_CONVERSATION_LUA,
                RScript.ReturnType.INTEGER,
                List.of(allKey(playerId), conversationKey(playerId, targetId), readTimeKey(playerId, targetId)),
                String.valueOf(System.currentTimeMillis() - KEEP_MILLIS), String.valueOf(readTime),
                String.valueOf(KEEP_SECONDS));
        return toCount(count);
    }

    public int count(long playerId) {
        Long count = script.eval(RScript.Mode.READ_WRITE, COUNT_LUA, RScript.ReturnType.INTEGER,
                List.of(allKey(playerId)), String.valueOf(System.currentTimeMillis() - KEEP_MILLIS));
        return toCount(count);
    }

    private int toCount(Long count) {
        return count == null ? 0 : Math.toIntExact(count);
    }

    private String allKey(long playerId) {
        return "social:private:unread:{%d}:all".formatted(playerId);
    }

    private String conversationKey(long playerId, long targetId) {
        return "social:private:unread:{%d}:conversation:%d".formatted(playerId, targetId);
    }

    private String readTimeKey(long playerId, long targetId) {
        return "social:private:unread:{%d}:read:%d".formatted(playerId, targetId);
    }
}
