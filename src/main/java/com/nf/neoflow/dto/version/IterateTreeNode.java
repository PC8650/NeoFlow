package com.nf.neoflow.dto.version;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nf.neoflow.constants.TimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 版本迭代树节点
 * @author PC8650
 */
@Data
public class IterateTreeNode {

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "允许退回发起的次数")
    private Integer cycle;

    @JsonIgnore
    @Schema(name = "是否为顶层节点")
    private Boolean top;

    @Schema(name = "创建人标识")
    private String createBy;

    @Schema(name = "创建人名称")
    private String createByName;

    @Schema(name = "创建时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime createTime;

    @Schema(name = "直系迭代数量")
    private Integer iterateCount;

    @Schema(name = "直系迭代版本号")
    private List<Integer> iterateVersion;

    @Schema(name = "迭代列表")
    private List<IterateTreeNode> iterate;
}
