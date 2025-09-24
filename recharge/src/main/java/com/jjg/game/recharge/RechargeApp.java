package com.jjg.game.recharge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * @author 11
 * @date 2025/9/22 19:28
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@ComponentScan(
        basePackages = "com.jjg.game",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                                com.jjg.game.core.manager.RedDotServiceRegistrar.class
                        }),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.jjg.game.core.dao.room.*")
        })
public class RechargeApp {
    public static void main(String[] args) {
        SpringApplication.run(RechargeApp.class, args);
    }
}
