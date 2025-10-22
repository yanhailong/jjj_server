package com.jjg.game.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * @author 11
 * @date 2025/5/24 14:30
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@ComponentScan(
    basePackages = "com.jjg.game",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.jjg.game.core.dao.room.*"),
    })
public class AccountApp {

    public static void main(String[] args) {
        SpringApplication.run(AccountApp.class, args);
    }
}
