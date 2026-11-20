package com.dayforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = "com.dayforge.entity")
@EnableJpaRepositories(basePackages = "com.dayforge.repository")
public class DayForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayForgeApplication.class, args);
    }
}

