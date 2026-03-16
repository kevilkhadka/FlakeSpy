package com.flakespy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlakespyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlakespyApplication.class, args);
    }
}
