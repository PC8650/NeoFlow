package com.nf.neoflow.dto.process;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更改流程启用状态
 * @author PC8650
 */
@Data
public class ProcessChangeActiveForm {

    @ApiModelProperty("流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String name;

    @ApiModelProperty("更改的状态：true-启用，false-关闭")
    @NotNull(message = "启用状态不能为空")
    private Boolean active;

    @ApiModelProperty("更新人标识")
    private String updateBy;

}
