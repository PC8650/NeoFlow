package com.nf.neoflow.constants;

/**
 * 流程实例状态枚举
 * @author PC8650
 */
public class InstanceStatus {

    /**
     * 进行中
     */
    public static final Integer PENDING = 1;

    /**
     * 已完成
     * 通过
     */
    public static final Integer COMPLETE = 2;

    /**
     * 已拒绝
     * 未通过
     */
    public static final Integer REJECTED = 3;

    /**
     * 已终止
     * 意外终止
     */
    public static final Integer TERMINATED = 4;
}
