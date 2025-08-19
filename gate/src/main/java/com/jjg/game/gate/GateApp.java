package com.jjg.game.gate;

import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 网关服务器启动类
 *
 * @author 11
 * @date 2025/5/23 16:46
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
        QuartzAutoConfiguration.class,
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        RedissonAutoConfigurationV2.class
})
@ComponentScan(
        basePackages = {"com.jjg.game"},
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.jjg.game.common.redis.*"))
public class GateApp {
    public static void main(String[] args) {
        SpringApplication.run(GateApp.class, args);
    }
}

