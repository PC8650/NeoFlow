package org.nf.neoflow.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流程实例操作历史查询表单
 */
@Data
public class OperationHistoryForm {

    @Schema(name = "业务key")
    @NotBlank(message = "业务key不能为空")
    private String businessKey;

    @Schema(name = "实例节点位置", minimum = "1", description = "该节点及以前的操作历史，为空则查询该流程实例的所有节点")
    @Min(value = 1, message = "节点位置不能小于1")
    private Integer num;

    @Schema(name = "是否查询指定节点变量数据", description = "默认false，num为空，则查询实例当前最新节点，否则为num指定的节点")
    private boolean variableData = false;
}
