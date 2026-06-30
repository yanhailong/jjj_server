package com.jjg.game.alliance.service;

import com.jjg.game.alliance.constant.AllianceConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

/**
 * 联盟缓存失效广播订阅 (范式对齐 {@code SocialRedisListenerConfig})。
 *
 * @author 11
 * @date 2026/6/11
 */
@Configuration
public class AllianceRedisListenerConfig {
    private static final Logger log = LoggerFactory.getLogger(AllianceRedisListenerConfig.class);

    @Bean
    public RedisMessageListenerContainer allianceRedisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                               AllianceCacheService cacheService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            try {
                cacheService.invalidateLocal(Long.parseLong(body));
            } catch (NumberFormatException e) {
                log.warn("忽略非法联盟缓存失效消息 body={}", body);
            }
        }, new ChannelTopic(AllianceConst.RedisKey.INVALIDATE_CHANNEL));
        //玩家->联盟映射失效: payload 为单个或逗号分隔的多个 playerId
        container.addMessageListener((message, pattern) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            for (String part : body.split(",")) {
                if (part.isEmpty()) {
                    continue;
                }
                try {
                    cacheService.invalidatePlayerLocal(Long.parseLong(part.trim()));
                } catch (NumberFormatException e) {
                    log.warn("忽略非法玩家联盟映射失效消息 part={}", part);
                }
            }
        }, new ChannelTopic(AllianceConst.RedisKey.PLAYER_INVALIDATE_CHANNEL));
        return container;
    }
}
