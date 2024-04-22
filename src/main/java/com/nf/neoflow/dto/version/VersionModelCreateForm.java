package com.nf.neoflow.dto.version;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

/**
 * 版本模型创建表单
 * @author PC8650
 */
@Data
public class VersionModelCreateForm {

    @ApiModelProperty("流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String processName;

    @ApiModelProperty("迭代自版本号，空白创建则为null")
    private Integer iterateFrom;

    @ApiModelProperty("拒绝后退回至发起的次数")
    private Integer cycle = 0;

    @ApiModelProperty("终止方法，对应的@ProcessMethod")
    private String terminatedMethod;

    @ApiModelProperty("创建人标识")
    private String createBy;

    @ApiModelProperty("创建人名称")
    private String createByName;

    @ApiModelProperty("流程模型边")
    @NotEmpty(message = "节点不能为空")
    private Set<ModelNodeDto> nodes;

    @ApiModelProperty("流程模型边")
    @NotEmpty(message = "边不能为空")
    private Set<ProcessNodeEdge> edges;
}
