package com.nf.neoflow.config;

import com.nf.neoflow.annotation.ProcessOperator;
import com.nf.neoflow.enums.AutoTypeEnum;
import com.nf.neoflow.interfaces.CustomizationLock;
import com.nf.neoflow.interfaces.UserChoose;
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
    private Integer autoType = AutoTypeEnum.TODAY.getCode();

    /**
     * 自带接口基路径配置
     */
    private String prefix;

    /**
     * 是否开启自带用户选择接口，用于在流程 "创建"、"修改"、"发起"、"审批" 操作中获取当前用户
     * {@link UserChoose UserChoose}
     */
    private Boolean baseUserChoose = false;

    /**
     * 是否开启自定义锁
     * {@link CustomizationLock CustomizationLock}
     */
    private Boolean CustomizationLock = false;
}
