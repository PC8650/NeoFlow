package org.nf.neoflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 注解扫描配置
 * @author PC8650
 */
@Data
@Component
@ConfigurationProperties(prefix = "neo")
public class NeoScanConfig {

    /**
     * 是否扫描注解 {@link org.nf.neoflow.annotation.ProcessOperator ProcessOperator} 和 {@link org.nf.neoflow.annotation.ProcessMethod ProcessMethod}
     * 默认 true
     */
    private Boolean scan = true;

    /**
     * 扫描的包名
     */
    private String scanPackage;
}
