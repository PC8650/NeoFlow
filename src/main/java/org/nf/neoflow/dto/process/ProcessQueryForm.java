package org.nf.neoflow.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProcessQueryForm {

    @Schema(name = "流程名称")
    private String name;

    @Schema(name = "创建人唯一标识")
    private String createBy;

    @Schema(name = "是否按创建时间降序排列", nullable = true, defaultValue = "false")
    private Boolean desc = false;

    @Schema(name = "页码")
    private Integer pageNumber = 1;

    @Schema(name = "每页显示数量")
    private Integer pageSize = 15;
}
