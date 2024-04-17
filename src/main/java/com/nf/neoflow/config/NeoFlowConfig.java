package com.nf.neoflow.config;

import com.nf.neoflow.constants.AutoType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义配置
 * @author PC8650
 */
@Data
@Component
@ConfigurationProperties(prefix = "neo")
public class NeoFlowConfig {

    /**
     * 是否独立于业务单独部署
     */
    private Boolean independence = false;

    /**
     * 扫描的包名
     */
    private String scanPackage = "com.nf.neoflow.dto";

    /**
     * 自动执行节点扫描周期Corn
     */
    private String autoCorn = "0 0 5 * * ?";

    /**
     * 自动节点执行类型
     * 1-只执行当天，2-执行当天及以前
     */
    private Integer autoType = AutoType.TODAY;

    /**
     * 自带接口基路径配置
     */
    private String prefix;

    /**
     * 是否开启自带用户选择接口，用于在流程 "创建"、"修改"、"发起"、"审批" 操作中获取当前用户
     * {@link com.nf.neoflow.interfaces.UserChoose UserChoose}
     */
    private Boolean baseUserChoose = false;

    /**
     * 是否开启自定义锁
     * {@link com.nf.neoflow.interfaces.CustomizationLock CustomizationLock}
     */
    private Boolean customizationLock = false;

    /**
     * 是否使用缓存
     */
    private Boolean enableCache = true;

    /**
     * 是否缓存空值，防止缓存穿透
     */
    private Boolean cacheNull = false;

    /**
     * 默认缓存策略初始化容量
     */
    private Integer initCacheCount = 50;

    /**
     * 默认缓存策略最大容量
     */
    private Integer maxCapacityCount = 500;

    /**
     * 默认缓存策略过期时间，单位：分钟
     */
    private Integer expire = 10;


    /**
     * 多段缓存key分隔符，默认 ':'
     */
    private char separate = ':';

    /**
     * 是否开启自定义缓存
     * {@link com.nf.neoflow.interfaces.CustomizationCache CustomizationLock}。
     * 默认使用本地缓存
     */
    private Boolean customizationCache = false;
}
