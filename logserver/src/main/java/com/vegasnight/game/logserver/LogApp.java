package com.vegasnight.game.logserver;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author 11
 * @date 2025/5/27 17:51
 */
@SpringBootApplication
@ComponentScan({"com.vegasnight.game"})
@EnableDubbo
public class LogApp {

    public static void main(String[] args) {
        SpringApplication.run(LogApp.class, args);
    }
}
