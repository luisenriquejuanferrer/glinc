package com.glinc.glincbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling para que GlucosePoller corra cada 90s.
@SpringBootApplication
@EnableScheduling
public class GlincBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlincBackendApplication.class, args);
    }

}
