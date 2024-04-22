package com.nf.neoflow.dto.execute;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 节点查询
 * @author PC8650
 */
@Data
public class NodeQueryDto<T> {

    @ApiModelProperty("节点")
    T node;

    @ApiModelProperty("映射查询")
    String nodeJson;

    @ApiModelProperty("上一节点状态")
    private Boolean before;

    @ApiModelProperty("节点与参数位置是否匹配")
    private Boolean match;

    @ApiModelProperty("版本")
    Integer version;

}
