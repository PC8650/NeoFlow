package org.nf.neoflow.constants;

/**
 * 流程实例操作类型
 * @author PC8650
 */
public class InstanceOperationType {

    /**
     * 发起
     */
    public static final Integer INITIATE = 1;

    /**
     * 同意
     */
    public static final Integer PASS = 2;

    /**
     * 拒绝
     */
    public static final Integer REJECTED = 3;

    /**
     * 转发
     */
    public static final Integer FORWARD = 4;

    /**
     * 终止
     */
    public static final Integer TERMINATED = 5;
}
