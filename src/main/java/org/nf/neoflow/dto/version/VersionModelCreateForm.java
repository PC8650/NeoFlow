package org.nf.neoflow.dto.version;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(name = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String processName;

    @Schema(name = "迭代自版本号", nullable = true, defaultValue = "空白创建则为null")
    private Integer iterateFrom;

    @Schema(name = "拒绝后退回至发起的次数", nullable = true, defaultValue = "0")
    private Integer cycle = 0;

    @Schema(name = "终止方法", nullable = true, description = "对应的@ProcessMethod")
    private String terminatedMethod;

    @Schema(name = "创建人标识", nullable = true, description = "NeoFlowConfig.baseUserChoose的配置选择")
    private String createBy;

    @Schema(name = "创建人名称", nullable = true, description = "NeoFlowConfig.baseUserChoose的配置选择")
    private String createByName;

    @Schema(name = "流程模型节点")
    @NotEmpty(message = "节点不能为空")
    private Set<ModelNodeDto> nodes;

    @Schema(name = "流程模型边")
    @NotEmpty(message = "边不能为空")
    private Set<ProcessNodeEdge> edges;
}
