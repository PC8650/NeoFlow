package com.nf.neoflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自动节点执行类型枚举
 * @author PC8650
 */
@Getter
@AllArgsConstructor
public enum AutoTypeEnum {


    TODAY(1, "只执行当天"),


    BEFORE(2, "执行当天及以前");


    final Integer code;


    final String message;

}
