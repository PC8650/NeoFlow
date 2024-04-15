package com.nf.neoflow.dto.version;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流程版本列表查询表单
 * @author PC8650
 */
@Data
public class VersionListQueryForm {

    @ApiModelProperty("流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String processName;

    @ApiModelProperty("是否时间倒序")
    private Boolean desc = false;

    @ApiModelProperty("页码")
    private Integer pageNumber = 1;

    @ApiModelProperty("每页显示数量")
    private Integer pageSize = 15;
}
