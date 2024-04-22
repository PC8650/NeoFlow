package com.nf.neoflow.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 事务配置
 * @author PC8650
 */
@Configuration
@AllArgsConstructor
public class TransactionConfig {

    private final PlatformTransactionManager transactionManager;

    @Bean
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }
}
