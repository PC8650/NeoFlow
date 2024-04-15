package com.nf.neoflow.dto.version;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 迭代树节点图形化数据
 *节点和迭代关系(不含坐标)，边用版本号连接节点
 * @author PC8650
 */
@Data
public class IterateTreeNodeGraphic {

    @ApiModelProperty("版本节点")
    private List<IterateTreeNode> nodes;

    @ApiModelProperty("迭代关系")
    private List<ProcessNodeEdge> edges;
}
