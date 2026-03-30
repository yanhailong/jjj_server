package com.jjg.game.slots;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * @author 11
 * @date 2025/7/24 16:45
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@EnableScheduling
@ComponentScan(
        basePackages = "com.jjg.game"
)
public class SlotsApp {
    public static void main(String[] args) {
        SpringApplication.run(SlotsApp.class, args);
    }
}
