package org.nf.neoflow.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更改流程启用状态
 * @author PC8650
 */
@Data
public class ProcessChangeActiveForm {

    @Schema(name = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String name;

    @Schema(name = "更改的状态", description = "true-启用，false-关闭")
    @NotNull(message = "启用状态不能为空")
    private Boolean active;

    @Schema(name = "更新人标识", nullable = true, description = "NeoFlowConfig.baseUserChoose的配置选择")
    private String updateBy;

}
