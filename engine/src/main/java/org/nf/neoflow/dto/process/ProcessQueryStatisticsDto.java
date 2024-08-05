package org.nf.neoflow.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 流程统计查询
 * @author PC8650
 */
public record ProcessQueryStatisticsDto(
        @Schema(name = "流程名称")
        String name,

        @Schema(name = "进行中的流程数量")
        Long pending,

        @Schema(name = "已完成的流程数量")
        Long complete,

        @Schema(name = "已拒绝的流程数量")
        Long rejected,

        @Schema(name = "已终止的流程数量")
        Long terminated,

        @Schema(name = "流程总数")
        Long total,

        @Schema(name = "版本信息")
        List<Map<String, Long>> version
) {
}
