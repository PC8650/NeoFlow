package com.nf.neoflow.dto.version;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nf.neoflow.models.ModelNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Value;

import java.util.Map;
import java.util.Set;

/**
 * 版本模型视图
 * @author PC8650
 */
@Data
@JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
public class VersionModelViewDto {

    @ApiModelProperty("流程名称")
    private String processName;

    @ApiModelProperty("迭代自版本号")
    private Integer iterateFrom;

    @ApiModelProperty("版本号")
    private Integer version;

    @ApiModelProperty("允许退回发起的次数")
    private Integer cycle;

    @ApiModelProperty("组件模型")
    private Map<String,Object> componentModel;

    @ApiModelProperty("版本节点")
    private Set<ModelNode> versionNodes;

    @ApiModelProperty("版本边")
    private Set<ProcessNodeEdge> versionEdges;

}
