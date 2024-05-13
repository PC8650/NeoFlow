package com.nf.neoflow.dto.version;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 版本视图查询
 * @author PC8650
 */
@Data
public class VersionViewQueryForm {

    @Schema(name = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String processName;

    @Schema(name = "版本号")
    private Integer version;
}
