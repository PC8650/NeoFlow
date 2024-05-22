package org.nf.neoflow.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 流程启用状态
 * @author PC8650
 */
public record ProcessActiveStatusDto(
        @Schema(name = "流程名称")
        String name,

        @Schema(name = "是否启用")
        Boolean active,

        @Schema(name = "启用版本")
        Integer activeVersion,

        @Schema(name = "备注信息")
        String remark
) {
}
