package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.demo","org.nf.neoflow"})
public class FlowDemo {

    public static void main(String[] args) {
        SpringApplication.run(FlowDemo.class, args);
    }
}
