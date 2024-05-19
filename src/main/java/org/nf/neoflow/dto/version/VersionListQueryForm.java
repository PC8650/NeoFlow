package org.nf.neoflow.dto.version;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流程版本列表查询表单
 * @author PC8650
 */
@Data
public class VersionListQueryForm {

    @Schema(name = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String processName;

    @Schema(name = "是否创建时间倒序", nullable = true, defaultValue = "false")
    private Boolean desc = false;

    @Schema(name = "页码", nullable = true, defaultValue = "1")
    private Integer pageNumber = 1;

    @Schema(name = "每页显示数量", nullable = true, defaultValue = "15")
    private Integer pageSize = 15;
}
