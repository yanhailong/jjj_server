package com.jjg.game.recharge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author 11
 * @date 2025/9/22 19:28
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@ComponentScan({"com.jjg.game"})
public class RechargeApp {
    public static void main(String[] args) {
        SpringApplication.run(RechargeApp.class, args);
    }
}
