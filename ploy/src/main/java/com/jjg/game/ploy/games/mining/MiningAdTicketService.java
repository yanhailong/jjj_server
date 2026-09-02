package com.jjg.game.ploy.games.mining;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/** 仅供广告平台签名/交易验证成功的服务端回调调用；不向客户端开放出票接口。 */
@Service
public class MiningAdTicketService {
    private final RedissonClient redis;
    private final MiningConfig config;
    public MiningAdTicketService(RedissonClient redis, MiningConfig config) { this.redis = redis; this.config = config; }

    public String issueVerifiedTicket(long playerId, String providerTransactionId, int day) {
        if (providerTransactionId == null || providerTransactionId.isBlank() || playerId <= 0)
            throw new IllegalArgumentException("Verified provider transaction required");
        String transaction = UUID.nameUUIDFromBytes(providerTransactionId.getBytes(StandardCharsets.UTF_8)).toString();
        RBucket<String> issued = redis.getBucket("mining:ad:transaction:" + transaction, StringCodec.INSTANCE);
        String ticket = UUID.randomUUID().toString();
        String record = playerId + ":" + day + ":" + ticket;
        if (!issued.setIfAbsent(record, Duration.ofDays(30))) {
            String existing = issued.get();
            if (existing == null || !existing.startsWith(playerId + ":" + day + ":"))
                throw new MiningException("AD_TRANSACTION_ALREADY_USED");
            // 不刷新票据TTL，重复回调不能延长或重新获取奖励。
            return existing.substring(existing.lastIndexOf(':') + 1);
        }
        redis.<String>getBucket("mining:ad:ticket:" + ticket, StringCodec.INSTANCE)
                .set(playerId + ":" + day, Duration.ofSeconds(config.adTicketSeconds));
        return ticket;
    }

    public boolean valid(long playerId, int day, String ticket) {
        if (ticket == null || !ticket.matches("[a-f0-9-]{36}")) return false;
        return (playerId + ":" + day).equals(redis.<String>getBucket("mining:ad:ticket:" + ticket, StringCodec.INSTANCE).get());
    }
}
