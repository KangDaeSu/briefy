package com.briefy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BriefyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BriefyApplication.class, args);
    }
}
