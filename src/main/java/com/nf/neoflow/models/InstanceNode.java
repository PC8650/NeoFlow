package com.nf.neoflow.models;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * InstanceNode 流程版本实例节点
 * @author PC8650
 */
@Data
@Node
public class InstanceNode {

    @Id
    @GeneratedValue
    private Long id;

    @ApiModelProperty("接收节点id")
    private Long nodeId;

    @ApiModelProperty("对应的模型节点id")
    private Long modelNodeId;

    @ApiModelProperty("节点名称")
    private String name;

    @ApiModelProperty("节点标识，可用于需对特殊节点做处理的业务")
    private String identity;

    @ApiModelProperty("节点状态：1-待处理，2-同意，3-拒绝，4-转发")
    private Integer status;

    @ApiModelProperty("节点操作候选人标识，配合operationType自定义")
    private List<String> operationCandidate;

    @ApiModelProperty("节点操作候选人名称，配合operationType自定义")
    private List<String> operationCandidateName;

    @ApiModelProperty("节点同意方法，对应的@ProcessMethod")
    private String agreeMethod;

    @ApiModelProperty("节点拒绝方法，对应的@ProcessMethod")
    private String refuseMethod;

    @ApiModelProperty("自动执行日期，有值将忽略操作类型和候选人")
    private LocalDate autoTime;

    @ApiModelProperty("实际操作人标识")
    private String operationBy;

    @ApiModelProperty("实际操作人名称")
    private String operationByName;

    @ApiModelProperty("节点开始时间")
    private LocalDateTime beginTime;

    @ApiModelProperty("节点结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("节点持续时间")
    private String during;

}
