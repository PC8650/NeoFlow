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
     * 自动节点每次执行数量，0则扫描并执行所有
     * 默认 0
     */
    private int autoCount = 0;

    /**
     * 自动节点执行类型
     * 1-只执行当天（默认），2-执行当天及以前
     */
    private int autoType = AutoType.TODAY;

    /**
     * 自带接口基路径配置
     * 默认 /neo
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
     * 默认缓存策略是否缓存空值，防止缓存穿透
     */
    private Boolean cacheNull = false;

    /**
     * 默认缓存策略初始化容量
     */
    private int initCacheCount = 50;

    /**
     * 默认缓存策略最大容量
     */
    private int maxCapacityCount = 500;

    /**
     * 默认缓存策略过期时间，单位：分钟
     */
    private int expire = 10;

    /**
     * 默认缓存策略下统计缓存过期时间，单位：秒
     */
    private long statisticExpire = 30;

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

    /**
     * 自动节点线程池核心线程池大小
     * 默认2 * CPU核数
     */
    private int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 自动节点线程池最大线程池大小
     * 默认3 * CPU核数
     */
    private int maxPoolSize = Runtime.getRuntime().availableProcessors() * 3;

    /**
     * 自动节点线程池队列容量
     * 默认100
     */
    private int queueCapacity = 100;

    /**
     * 自动节点线程池线程存活时间，单位：秒
     * 默认 60
     */
    private int keepAliveTime = 60;

    /**
     * 自动节点线程池队列类型
     * 默认 linked
     * array: ArrayBlockingQueue
     * linked: LinkedBlockingQueue
     * synchronous: SynchronousQueue(设置不公平)
     * priority: PriorityBlockingQueue
     */
    private String queueType = "linked";

    /**
     * 自动节点线程池拒绝策略
     * 默认 caller-runs
     * abort：AbortPolicy，丢弃任务并抛出RejectedExecutionException异常
     * caller-runs：CallerRunsPolicy，由调用线程处理该任务
     * discard：DiscardPolicy，丢弃任务，但是不抛出异常
     * discard-oldest：DiscardOldestPolicy，丢弃队列中最老的任务
     */
    private String rejectionPolicy = "caller-runs";

    /**
     * 自动节点线程池使用虚拟线程（将只支持最大线程数的配置）
     * 默认 false
     */
    private Boolean enableVirtual = false;

    /**
     * 虚拟线程池达到上线后，新任务等待时间，单位：秒
     * 默认 5
     */
    private int virtualAwaitTime = 5;

    /**
     * 执行自动节点时的操作人id
     * 默认 system
     */
    private String autoId = "system";

    /**
     * 执行自动节点时的操作人名称
     * 默认 system
     */
    private String autoName = "system";
}
