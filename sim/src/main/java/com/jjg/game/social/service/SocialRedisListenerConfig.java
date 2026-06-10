package com.jjg.game.social.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
public class SocialRedisListenerConfig {
    private static final Logger log = LoggerFactory.getLogger(SocialRedisListenerConfig.class);

    @Bean
    public RedisMessageListenerContainer socialRedisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                            SocialRelationCache relationCache) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            try {
                relationCache.invalidateLocal(Long.parseLong(body));
            } catch (NumberFormatException e) {
                log.warn("忽略非法黑名单缓存失效消息 body={}", body);
            }
        }, new ChannelTopic(SocialRelationCache.BLACKLIST_INVALIDATE_CHANNEL));
        return container;
    }
}
