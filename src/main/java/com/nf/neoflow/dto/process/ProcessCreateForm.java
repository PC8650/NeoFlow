package com.nf.neoflow.dto.process;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流程创建表单
 * @author PC8650 
 */
@Data
public class ProcessCreateForm {

    @NotBlank(message = "流程名称不能为空")
    @ApiModelProperty("流程名称")
    private String name;

    @ApiModelProperty("创建人唯一标识")
    private String createBy;
    
}
