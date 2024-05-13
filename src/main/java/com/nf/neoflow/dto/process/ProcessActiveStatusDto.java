package com.nf.neoflow.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程启用状态
 * @author PC8650
 */
@Data
public class ProcessActiveStatusDto {

    @Schema(name = "流程名称")
    private String name;

    @Schema(name = "是否启用")
    private Boolean active;

    @Schema(name = "启用版本")
    private Integer activeVersion;

    @Schema(name = "备注信息")
    private String remark;
}
