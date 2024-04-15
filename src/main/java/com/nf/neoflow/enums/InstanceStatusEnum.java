package com.nf.neoflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程实例状态枚举
 * @author PC8650
 */
@Getter
@AllArgsConstructor
public enum InstanceStatusEnum {

    /**
     * 进行中
     */
    ING(1, "进行中"),
    /**
     * 已完成
     * 通过
     */
    COMPLETE(2, "已完成"),
    /**
     * 已拒绝
     * 未通过
     */
    REJECTED(3, "已拒绝"),
    /**
     * 已终止
     * 意外终止
     */
    TERMINATED(4, "已终止");

    private final Integer code;
    private final String message;
}
