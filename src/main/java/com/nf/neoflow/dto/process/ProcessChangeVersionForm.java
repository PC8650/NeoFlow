package com.nf.neoflow.dto.process;

import io.swagger.annotations.ApiModelProperty;
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

    @ApiModelProperty("流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String name;

    @ApiModelProperty("更换的版本号")
    @NotNull(message = "版本号不能为空")
    @Min(value = 1, message = "版本号不能小于1")
    private Integer activeVersion;

    @ApiModelProperty("更新人标识")
    private String updateBy;

    @ApiModelProperty("更新人名称")
    private String updateByName;

}
