package com.jjg.game.sim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author 11
 * @date 2026/5/25
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@EnableScheduling
@ComponentScan(
        basePackages = "com.jjg.game"
)
public class SimApp {
    public static void main(String[] args) {
        SpringApplication.run(SimApp.class, args);
    }
}
