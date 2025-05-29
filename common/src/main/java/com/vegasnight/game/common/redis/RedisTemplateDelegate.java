package com.vegasnight.game.common.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @since 1.0
 */
public class RedisTemplateDelegate<K, V> extends RedisTemplate<K, V> {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void setEnableTransactionSupport(boolean enableTransactionSupport) {
        try {
            if (enableTransactionSupport) {
                RedisConnectionUtils.bindConnection(getConnectionFactory());
                //log.debug("redis 开始事务执行，进行连接绑定");
            } else {
                RedisConnectionUtils.unbindConnection(getConnectionFactory());
                //log.debug("redis 事务执行完毕，取消连接线程绑定");
            }
        } catch (Exception e) {
            log.warn("redis 连接绑定错误", e);
        }
    }
}
