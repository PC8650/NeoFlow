package org.nf.neoflow.constants;

/**
 * 流程实例节点状态
 * @author PC8650
 */
public class InstanceNodeStatus {

    /**
     * 待办
     */
    public static final Integer PENDING = 1;

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
