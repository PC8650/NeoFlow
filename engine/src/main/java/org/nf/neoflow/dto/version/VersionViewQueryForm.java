package org.nf.neoflow.dto.version;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 版本视图查询
 * @author PC8650
 */
public record VersionViewQueryForm(
        @Schema(name = "流程名称")
        @NotBlank(message = "流程名称不能为空")
        String processName,

        @Schema(name = "版本号", nullable = true, defaultValue = "1")
        @Min(value = 1, message = "版本号不能小于1")
        Integer version
) {
}
