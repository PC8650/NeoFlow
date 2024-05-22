package org.nf.neoflow.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程查询表单
 * @author PC8650
 */
@Data
public class ProcessQueryForm {

    @Schema(name = "流程名称", nullable = true)
    private String name;

    @Schema(name = "创建人唯一标识", nullable = true)
    private String createBy;

    @Schema(name = "是否按创建时间降序排列", nullable = true, defaultValue = "false")
    private Boolean desc = false;

    @Schema(name = "页码", nullable = true, defaultValue = "1")
    private Integer pageNumber = 1;

    @Schema(name = "每页显示数量", nullable = true, defaultValue = "15")
    private Integer pageSize = 15;
}
