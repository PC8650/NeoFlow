package org.nf.neoflow.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

/**
 * 查询当前用户各流程待办数量表单
 */
public record OperatorOfPendingForm (
        @Schema(name = "流程名称", nullable = true)
        String name,

        @Schema(name = "流程版本", nullable = true, minimum = "1")
        @Min(value = 1, message = "流程版本必须大于等于1")
        Integer version,

        @Schema(name = "当前用户id", nullable = true, description = "NeoFlowConfig.baseUserChoose的配置选择")
        String userId,

        @Schema(name = "当前用户名称", nullable = true, description = "NeoFlowConfig.baseUserChoose的配置选择")
        String username
) {
}
