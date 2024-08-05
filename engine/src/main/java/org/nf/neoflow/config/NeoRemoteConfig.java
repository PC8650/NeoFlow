package org.nf.neoflow.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 远程调用业务配置
 * @author PC8650
 */
@Slf4j
@Getter
@Component
@ConditionalOnProperty(value = "neo.independence", havingValue = "true", matchIfMissing = false)
@ConfigurationProperties(prefix = "neo.remote")
public class NeoRemoteConfig {

    private final Map<String,String> group = new HashMap<>();

    @PostConstruct
    private void log() {
        log.info("远程调用流程分组：{}", group);
    }

}
