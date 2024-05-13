package com.nf.neoflow.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 查询当前用户各流程待办数量表单
 */
@Data
public class OperatorOfPendingForm {

    @Schema(name = "流程名称", nullable = true)
    private String name;

    @Schema(name = "流程版本", nullable = true, minimum = "1")
    @Min(value = 1, message = "流程版本必须大于等于1")
    private Integer version;

    @Schema(name = "当前用户id", nullable = true, description = "Neo4jConfig.baseUserChoose的配置选择")
    private String userId;

    @Schema(name = "当前用户名称", nullable = true, description = "Neo4jConfig.baseUserChoose的配置选择")
    private String username;
}
