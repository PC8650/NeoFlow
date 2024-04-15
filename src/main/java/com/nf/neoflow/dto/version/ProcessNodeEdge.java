package com.nf.neoflow.dto.version;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 流程节点边
 * @author PC8650
 */
@Data
public class ProcessNodeEdge {

    @ApiModelProperty("开始节点标识")
    String startNode;

    @ApiModelProperty("结束节点标识")
    String endNode;

    @ApiModelProperty("条件")
    Integer condition;

}
