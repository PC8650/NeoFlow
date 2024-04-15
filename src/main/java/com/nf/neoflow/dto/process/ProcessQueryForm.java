package com.nf.neoflow.dto.process;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ProcessQueryForm {

    @ApiModelProperty("流程名称")
    private String name;

    @ApiModelProperty("创建人唯一标识")
    private String createBy;

    @ApiModelProperty("是否按创建时间降序排列")
    private Boolean desc = false;

    @ApiModelProperty("页码")
    private Integer pageNumber = 1;

    @ApiModelProperty("每页显示数量")
    private Integer pageSize = 15;
}
