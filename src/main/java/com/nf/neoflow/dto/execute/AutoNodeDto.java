package com.nf.neoflow.dto.execute;

import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

/**
 * 自动执行节点数据
 * @author PC8650
 */
public record AutoNodeDto (
        @ApiModelProperty("流程名称") String processName,
        @ApiModelProperty("版本") Integer version,
        @ApiModelProperty("业务key") String businessKey,
        @ApiModelProperty("实例节点位置") Integer num,
        @ApiModelProperty("实例节点id") Long nodeId,
        @ApiModelProperty("模型节点uid") String modelNodeUid,
        @ApiModelProperty("节点执行方法") String operationMethod,
        @ApiModelProperty("节点位置标识") Integer location,
        @ApiModelProperty("默认通过时的跳转条件") Integer defaultPassCondition,
        @ApiModelProperty("节点开始时间") LocalDateTime beginTime){
}
