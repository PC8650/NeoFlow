package com.nf.neoflow.models;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @ApiModelProperty("对应的模型节点uid")
    private String modelNodeUid;

    @ApiModelProperty("节点名称")
    private String name;

    @ApiModelProperty("节点标识，可用于需对特殊节点做处理的业务")
    private String identity;

    @ApiModelProperty("节点状态：1-待处理，2-同意，3-拒绝，4-转发")
    private Integer status;

    @ApiModelProperty("节点操作类型，根据业务自定义")
    private Integer operationType;

    @ApiModelProperty("指定节点操作候选人，配合operationType自定义")
    private String operationCandidate;

    @ApiModelProperty("接收传递候选人信息")
    private List<Map<String,Object>> operationCandidateInfo;

    @ApiModelProperty("节点操作方法，对应的@ProcessMethod")
    private String operationMethod;

    @ApiModelProperty("是否只通过才执行方法")
    private Boolean onlyPassExecute;

    @ApiModelProperty("自动执行日期，有值将忽略操作类型和候选人")
    private LocalDate autoTime;

    @ApiModelProperty("节点位置：1-开始，2-中间，3-完成，4-终止")
    private Integer location;

    @ApiModelProperty("实际操作人")
    private String operationBy;

    @ApiModelProperty("节点开始时间")
    private LocalDateTime beginTime;

    @ApiModelProperty("节点结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("节点持续时间：秒")
    private Long during;

}
