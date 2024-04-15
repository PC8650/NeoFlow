package com.nf.neoflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class NeoFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(NeoFlowApplication.class, args);
    }

}
