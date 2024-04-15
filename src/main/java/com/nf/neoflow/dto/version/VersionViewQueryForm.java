package com.nf.neoflow.dto.version;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 版本视图查询
 * @author PC8650
 */
@Data
public class VersionViewQueryForm {

    @ApiModelProperty("流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String processName;

    @ApiModelProperty("版本号")
    private Integer version;
}
