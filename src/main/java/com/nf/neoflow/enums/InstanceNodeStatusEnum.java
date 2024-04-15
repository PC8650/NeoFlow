package com.nf.neoflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程实例节点状态
 * @author PC8650
 */
@Getter
@AllArgsConstructor
public enum InstanceNodeStatusEnum {

    PENDING(1, "待处理"),
    AGREED(2, "同意"),
    REJECTED(3, "拒绝"),
    FORWARD(4, "转发"),
    TERMINATED(5, "终止");

    private final Integer code;
    private final String message;
}
