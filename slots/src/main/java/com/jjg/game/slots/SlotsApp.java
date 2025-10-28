package com.jjg.game.slots;

import com.jjg.game.slots.config.ExcludeServiceFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;


/**
 * @author 11
 * @date 2025/7/24 16:45
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@ComponentScan(
        basePackages = "com.jjg.game",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.CUSTOM,
                        classes = ExcludeServiceFilter.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.jjg.game.core.dao.room.*")
        })
public class SlotsApp {
    public static void main(String[] args) {
        SpringApplication.run(SlotsApp.class, args);
    }
}
