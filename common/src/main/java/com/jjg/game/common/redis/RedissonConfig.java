package com.jjg.game.common.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisson配置
 *
 * @author 2CL
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisAddress;
    @Value("${spring.data.redis.port}")
    private int redisPort;
    @Value("${spring.data.redis.password}")
    private String redisPassword;
    @Value("${spring.data.redis.database}")
    private int redisDb;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String redissonAddr = "redis://" + redisAddress + ":" + redisPort;
        config.useSingleServer().setAddress(redissonAddr);
        config.useSingleServer().setPassword(redisPassword);
        config.useSingleServer().setDatabase(redisDb);
        config.setCodec(StringCodec.INSTANCE);
        return Redisson.create(config);
    }
}
