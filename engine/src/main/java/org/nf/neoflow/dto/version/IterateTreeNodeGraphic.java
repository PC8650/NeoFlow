package org.nf.neoflow.dto.version;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 迭代树节点图形化数据
 *节点和迭代关系(不含坐标)，边用版本号连接节点
 * @author PC8650
 */
public record IterateTreeNodeGraphic(
        @Schema(name = "版本节点")
        List<IterateTreeNode> nodes,

        @Schema(name = "迭代关系")
        List<ProcessNodeEdge> edges
){
}
