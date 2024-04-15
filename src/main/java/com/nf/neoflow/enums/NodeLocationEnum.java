package com.nf.neoflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点位置类型枚举
 * @author PC8650
 */
@Getter
@AllArgsConstructor
public enum NodeLocationEnum {

    BEGIN(1, "开始"),
    MIDDLE(2, "中间"),
    COMPLETE(3, "完成"),
    TERMINATE(4, "终止");

    private final Integer code;
    private final String remark;
}
