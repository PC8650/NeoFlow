package com.nf.neoflow.models;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.List;
import java.util.Map;

/**
 * ModelNode 流程模型节点
 * @author PC8650
 */
@Data
@Node
public class ModelNode {

    @Id
    @GeneratedValue
    private Long id;

    @ApiModelProperty("创建模型时前端生成的uid")
    private String nodeUid;

    @ApiModelProperty("节点名称")
    private String name;

    @ApiModelProperty("节点标识，可用于需对特殊节点做处理的业务")
    private String identity;

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

    @ApiModelProperty("自动执行间隔，只精确到日期（x 天后，x <= 0 立即自动执行），有值将忽略操作类型和候选人")
    private Integer autoInterval;

    @ApiModelProperty("节点位置：1-开始，2-中间，3-完成，4-终止")
    private Integer location;

    @ApiModelProperty("x坐标")
    private Double x;

    @ApiModelProperty("y坐标")
    private Double y;

    /**
     * 复制忽略属性
     * @return String[]
     */
    public String[] ignoreCopyPropertyList() {
        return new String[]{
                "id","nodeUid",
                "operationCandidate",
                "operationCandidateInfo",
                "autoInterval",
                "x","y"
        };
    }

}
