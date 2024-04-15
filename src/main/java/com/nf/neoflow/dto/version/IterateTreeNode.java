package com.nf.neoflow.dto.version;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nf.neoflow.constants.TimeFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 版本迭代树节点
 * @author PC8650
 */
@Data
public class IterateTreeNode {

    @ApiModelProperty("版本号")
    private Integer version;

    @ApiModelProperty("允许退回发起的次数")
    private Integer cycle;

    @JsonIgnore
    @ApiModelProperty("是否为顶层节点")
    private Boolean top;

    @ApiModelProperty("创建人标识")
    private String createBy;

    @ApiModelProperty("创建人名称")
    private String createByName;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime createTime;

    @ApiModelProperty("直系迭代数量")
    private Integer iterateCount;

    @ApiModelProperty("直系迭代版本号")
    private List<Integer> iterateVersion;

    @ApiModelProperty("迭代列表")
    private List<IterateTreeNode> iterate;
}
