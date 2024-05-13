package com.nf.neoflow.dto.execute;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 节点查询
 * @author PC8650
 */
@Data
public class NodeQueryDto<T> {

    @Schema(name = "节点")
    T node;

    @Schema(name = "映射查询")
    String nodeJson;

    @Schema(name = "上一节点状态")
    private Boolean before;

    @Schema(name = "版本")
    Integer version;

    @Schema(name = "版本对应的终止方法")
    String terminatedMethod;

}
