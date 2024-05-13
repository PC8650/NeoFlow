package com.nf.neoflow.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程统计查询
 * @author PC8650
 */
@Data
public class ProcessQueryStatisticsDto {

    @Schema(name = "流程名称")
    private String name;

    @Schema(name = "进行中的流程数量")
    private Long pending;

    @Schema(name = "已完成的流程数量")
    private Long complete;

    @Schema(name = "已拒绝的流程数量")
    private Long rejected;

    @Schema(name = "已终止的流程数量")
    private Long terminated;

    @Schema(name = "流程总数")
    private Long total;

    @Schema(name = "版本信息")
    private List<Map<String, Long>> version;
}
