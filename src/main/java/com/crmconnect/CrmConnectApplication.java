package com.crmconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CrmConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmConnectApplication.class, args);
    }
}

