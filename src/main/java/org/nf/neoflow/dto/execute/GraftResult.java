package org.nf.neoflow.dto.execute;


import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 移植结果
 * @author PC8650
 */
public record GraftResult (
        @Schema(name = "流程名称") String processName,
        @Schema(name = "实例节点版本") Integer version,
        @Schema(name = "实例节点位置") Integer num,
        @Schema(name = "实例节点id") Long nodeId,
        @Schema(name = "实例节点业务key") String businessKey) {}
