package com.jjg.game.gm;

import com.jjg.game.gm.config.ExcludeServiceFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * @author 11
 * @date 2025/5/29 10:02
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@ComponentScan(
        basePackages = "com.jjg.game",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.CUSTOM,
                        classes = ExcludeServiceFilter.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.jjg.game.core.dao.room.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.jjg.game.slots\\..*")
        })
public class GmApp  {
    public static void main(String[] args) {
        SpringApplication.run(GmApp.class, args);
    }
}
