package com.jjg.game.ploy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author 11
 * @date 2026/5/9
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@EnableScheduling
@ComponentScan(
        basePackages = "com.jjg.game"
)
public class PloyApp {
    public static void main(String[] args) {
        SpringApplication.run(PloyApp.class, args);
    }
}