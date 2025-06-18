package com.jjg.game.hall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author 11
 * @date 2025/5/26 15:26
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@ComponentScan({"com.jjg.game"})
public class HallApp {

    public static void main(String[] args) {
        SpringApplication.run(HallApp.class, args);
    }

}
