package com.jjg.game.common.redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.utils.ObjectMapperUtil;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis配置类
 * <p>
 * 功能说明：
 * 1. 配置Redis缓存管理器，支持Spring Cache注解
 * 2. 自定义缓存键生成策略
 * 3. 配置RedisTemplate序列化方式
 * 4. 解决缓存转换异常和乱码问题
 * <p>
 * 注意：使用CachingConfigurer接口替代已弃用的CachingConfigurerSupport
 *
 * @since 1.0
 */
@Configuration
@EnableAutoConfiguration
public class RedisConfig implements CachingConfigurer {
    /**
     * 自定义缓存键生成器
     * <p>
     * 生成规则：类名 + 方法名 + 参数值
     * 例如：com.example.UserService.getUserById123
     *
     * @return KeyGenerator 缓存键生成器
     */
    @Bean
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getName());  // 添加类名
            sb.append(method.getName());              // 添加方法名
            for (Object obj : params) {
                sb.append(obj.toString());           // 添加参数值
            }
            return sb.toString();
        };
    }

    /**
     * 配置Redis缓存管理器
     * <p>
     * 功能说明：
     * 1. 解决查询缓存转换异常问题
     * 2. 配置序列化方式，解决乱码问题
     * 3. 设置缓存过期时间为600秒
     * 4. 禁用null值缓存，避免缓存空值
     *
     * @param factory Redis连接工厂
     * @return CacheManager Redis缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // 使用项目统一的ObjectMapper配置
        ObjectMapper mapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // 忽略未知字段

        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        // 配置序列化(解决乱码问题)，过期时间600秒
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(600))  // 设置缓存过期时间600秒
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))  // key使用String序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))  // value使用JSON序列化
                .disableCachingNullValues();  // 禁用null值缓存
        return RedisCacheManager.builder(factory).cacheDefaults(configuration).build();
    }

    /**
     * 配置RedisTemplate
     * <p>
     * 功能说明：
     * 1. 配置Redis操作模板的序列化方式
     * 2. key使用String序列化，便于查看和调试
     * 3. value使用JSON序列化，支持复杂对象存储
     * 4. Hash结构也使用相同的序列化策略
     *
     * @param factory Redis连接工厂
     * @return RedisTemplate Redis操作模板
     */
    @Bean("redisTemplate")
    public <T> RedisTemplate<String, T> redisTemplate(RedisConnectionFactory factory) {
        ObjectMapper mapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // 忽略未知字段

        RedisTemplate<String, T> template = new RedisTemplate<>();
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        template.setConnectionFactory(factory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // key序列化方式 - 使用String序列化，便于查看和调试
        template.setKeySerializer(stringRedisSerializer);
        // value序列化 - 使用JSON序列化，支持复杂对象
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // Hash key序列化
        template.setHashKeySerializer(jackson2JsonRedisSerializer);
        // Hash value序列化
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        return template;
    }

}
