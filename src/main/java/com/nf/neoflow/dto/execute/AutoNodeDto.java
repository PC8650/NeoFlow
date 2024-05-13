package com.nf.neoflow.dto.execute;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 自动执行节点数据
 * @author PC8650
 */
public record AutoNodeDto (
        @Schema(name = "流程名称") String processName,
        @Schema(name = "版本") Integer version,
        @Schema(name = "业务key") String businessKey,
        @Schema(name = "实例节点位置") Integer num,
        @Schema(name = "实例节点id") Long nodeId,
        @Schema(name = "模型节点uid") String modelNodeUid,
        @Schema(name = "节点执行方法") String operationMethod,
        @Schema(name = "节点位置标识") Integer location,
        @Schema(name = "默认通过时的跳转条件") Integer defaultPassCondition,
        @Schema(name = "节点开始时间") LocalDateTime beginTime){
}
