package org.nf.neoflow.dto.version;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import org.nf.neoflow.constants.TimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 版本迭代树节点
 * @author PC8650
 */
public record IterateTreeNode(
        @Schema(name = "版本号")
        Integer version,

        @Schema(name = "允许退回发起的次数")
        Integer cycle,

        @JsonIgnore
        @Schema(name = "是否为顶层节点")
        Boolean top,

        @Schema(name = "终止方法")
        String terminatedMethod,

        @Schema(name = "创建人标识")
        String createBy,

        @Schema(name = "创建人名称")
        String createByName,

        @Schema(name = "创建时间")
        @JsonFormat(pattern = TimeFormat.DATE_TIME)
        LocalDateTime createTime,

        @Schema(name = "直系迭代数量")
        Integer iterateCount,

        @Schema(name = "直系迭代版本号")
        List<Integer> iterateVersion,

        @Schema(name = "迭代列表")
        List<IterateTreeNode> iterate
) {
}
