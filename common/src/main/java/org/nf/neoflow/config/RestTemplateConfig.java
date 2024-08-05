package org.nf.neoflow.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @ConditionalOnProperty(value = "neo.independence", havingValue = "true", matchIfMissing = false)
    public RestTemplate setBean() {
        return new RestTemplate();
    }
}
