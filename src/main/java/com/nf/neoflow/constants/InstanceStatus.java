package com.nf.neoflow.constants;

/**
 * 流程实例状态枚举
 * @author PC8650
 */
public class InstanceStatus {

    /**
     * 进行中
     */
    private static final Integer PENDING = 1;

    /**
     * 已完成
     * 通过
     */
    private static final Integer COMPLETE = 1;

    /**
     * 已拒绝
     * 未通过
     */
    private static final Integer REJECTED = 1;

    /**
     * 已终止
     * 意外终止
     */
    private static final Integer TERMINATED = 1;
}
