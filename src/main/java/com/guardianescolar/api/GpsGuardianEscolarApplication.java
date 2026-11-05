package com.guardianescolar.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GpsGuardianEscolarApplication {

    public static void main(String[] args) {
        SpringApplication.run(GpsGuardianEscolarApplication.class, args);
    }
}
