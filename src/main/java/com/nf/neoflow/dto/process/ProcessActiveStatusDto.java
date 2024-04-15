package com.nf.neoflow.dto.process;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 流程启用状态
 * @author PC8650
 */
@Data
public class ProcessActiveStatusDto {

    @ApiModelProperty("流程名称")
    private String name;

    @ApiModelProperty("是否启用")
    private Boolean active;

    @ApiModelProperty("启用版本")
    private Integer activeVersion;

    private String remark;
}
