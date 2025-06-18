package com.jjg.game.gm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author 11
 * @date 2025/5/29 10:02
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@ComponentScan({"com.jjg.game"})
public class GmApp  {
    public static void main(String[] args) {
        SpringApplication.run(GmApp.class, args);
    }
}
