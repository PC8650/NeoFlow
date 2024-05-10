package com.nf.neoflow.dto.process;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程统计查询
 * @author PC8650
 */
@Data
public class ProcessQueryStatisticsDto {

    @ApiModelProperty("流程名称")
    private String name;

    @ApiModelProperty("进行中的流程数量")
    private Long pending;

    @ApiModelProperty("已完成的流程数量")
    private Long complete;

    @ApiModelProperty("已拒绝的流程数量")
    private Long rejected;

    @ApiModelProperty("已终止的流程数量")
    private Long terminated;

    @ApiModelProperty("流程总数")
    private Long total;

    @ApiModelProperty("版本信息")
    private List<Map<String, Long>> version;
}
