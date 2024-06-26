package org.nf.neoflow.config;

import lombok.Data;
import org.nf.neoflow.constants.AutoType;
import org.nf.neoflow.enums.LockEnums;
import org.nf.neoflow.interfaces.CustomizationCache;
import org.nf.neoflow.interfaces.CustomizationLock;
import org.nf.neoflow.interfaces.UserChoose;
import org.nf.neoflow.models.InstanceNode;
import org.nf.neoflow.models.ModelNode;
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

    //基础 Basic

    /**
     * 是否独立于业务单独部署
     */
    private Boolean independence = false;

    /**
     * 扫描的包名
     */
    private String scanPackage;

    /**
     * 自带接口基路径配置
     * 默认 /neo
     */
    private String prefix;

    /**
     * 节点约束和索引文件(不包含全文索引)，通过ResourceLoader获取
     * 默认 classpath:cypher/constraintAndIndex.cypher
     */
    private String constraintAndIndexFile = "classpath:cypher/constraintAndIndex.cypher";

    /**
     * 是否开启自带用户选择接口，用于在流程 "创建"、"修改"、"发起"、"审批" 操作中获取当前用户
     * 否则默认从前端传递
     * {@link UserChoose UserChoose}
     */
    private Boolean baseUserChoose = false;

    /**
     * 发起人标识，表示该节点候选人为流程发起人
     * 默认 0
     * 配合 {@link ModelNode ModelNode}、{@link InstanceNode InstanceNode} 的 operationType 字段
     */
    private int initiatorFlag = 0;

    /**
     *  {@link InstanceNode#getDuring() InstanceNode.during} 是否使用首字母缩写格式
     *  true：xDxHxMxS
     *  false：x天x时x分x秒
     *  默认false
     */
    private Boolean initialsDuring = true;

    /**
     * 流程批量操作上限
     * 默认 5
     */
    private int batchSize = 5;

    //锁 Lock

    /**
     * 是否开启自定义锁
     * {@link CustomizationLock CustomizationLock}
     */
    private Boolean customizationLock = false;


    //缓存 Cache

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
     * 默认 50
     */
    private int initCacheCount = 50;

    /**
     * 默认缓存策略最大容量
     * 默认 500
     */
    private int maxCapacityCount = 500;

    /**
     * 默认缓存策略过期时间，单位：分钟
     */
    private int expire = 10;

    /**
     * 默认缓存策略下统计缓存过期时间，单位：秒
     * 默认 30
     */
    private long statisticExpire = 30;

    /**
     * 多段缓存key分隔符，默认 ':'
     */
    private char separate = ':';

    /**
     * 是否开启自定义缓存
     * {@link CustomizationCache CustomizationLock}。
     * 默认使用本地缓存
     */
    private Boolean customizationCache = false;


    //自动节点 AutoNode

    /**
     * 判断{@link LockEnums#AUTO_EXECUTE LockEnums.AUTO_EXECUTE}是否满足释放条件的循环周期，单位：秒
     * 默认3s
     */
    private Integer autoLockCheckPeriod = 3;

    /**
     * {@link LockEnums#AUTO_EXECUTE LockEnums.AUTO_EXECUTE}的过期时间，单位：秒
     * 默认为空，无过期时间
     */
    private Integer autoLockExpired;

    /**
     * 是否开启自动节点扫描的定时任务
     * 默认 false
     */
    private Boolean scanAuto = false;

    /**
     * 自动执行节点扫描周期Corn
     * 默认 0 0 5 * * ?
     */
    private String autoCorn;

    /**
     * 自动节点每次执行数量，0则扫描并执行所有
     * 默认 0
     */
    private int autoCount = 0;

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

    /**
     * 自动节点执行类型
     * 1-只执行当天（默认），2-执行当天及以前
     */
    private int autoType = AutoType.TODAY;

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
     * 虚拟线程池达到上限后，新任务等待时间，单位：秒
     * 默认 5
     */
    private int virtualAwaitTime = 5;

    /**
     * 每条线程分配多少个自动节点
     * 默认 50
     */
    private int autoAssigned = 50;

}
