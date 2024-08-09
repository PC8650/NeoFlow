package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.demo","org.nf.neoflow"})
public class BusinessDemo {

    public static void main(String[] args) {
        SpringApplication.run(BusinessDemo.class, args);
    }
}
