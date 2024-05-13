package com.nf.neoflow.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流程创建表单
 * @author PC8650 
 */
@Data
public class ProcessCreateForm {

    @NotBlank(message = "流程名称不能为空")
    @Schema(name = "流程名称")
    private String name;

    @Schema(name = "创建人唯一标识", nullable = true, description = "Neo4jConfig.baseUserChoose的配置选择")
    private String createBy;
    
}
