package com.nf.neoflow.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更改流程版本
 * @author PC8650
 */
@Data
public class ProcessChangeVersionForm {

    @Schema(name = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String name;

    @Schema(name = "更换的版本号", minimum = "1")
    @NotNull(message = "版本号不能为空")
    @Min(value = 1, message = "版本号不能小于1")
    private Integer activeVersion;

    @Schema(name = "更新人标识", nullable = true, description = "Neo4jConfig.baseUserChoose的配置选择")
    private String updateBy;

    @Schema(name = "更新人名称", nullable = true, description = "Neo4jConfig.baseUserChoose的配置选择")
    private String updateByName;

}
