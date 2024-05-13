package com.nf.neoflow.dto.process;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nf.neoflow.constants.TimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

/**
 * 流程统计查询表单
 */
@Data
public class ProcessQueryStatisticsForm {

    @Schema(name = "流程名称", nullable = true)
    private String name;

    @Schema(name = "流程版本", nullable = true, minimum = "1")
    @Min(value = 1, message = "版本号不能小于1")
    private Integer version;

    @Schema(name = "流程开始时间起始", nullable = true)
    @JsonFormat(pattern = TimeFormat.DATE)
    private LocalDate beginStart;

    @Schema(name = "流程开始时间结束", nullable = true)
    @JsonFormat(pattern = TimeFormat.DATE)
    private LocalDate beginEnd;

    @Schema(name = "流程结束时间起始", nullable = true)
    @JsonFormat(pattern = TimeFormat.DATE)
    private LocalDate endStart;

    @Schema(name = "流程结束时间结束", nullable = true)
    @JsonFormat(pattern = TimeFormat.DATE)
    private LocalDate endEnd;

    @Schema(name = "最低流程进行数", nullable = true)
    private Long pending;

    @Schema(name = "最低流程完成数", nullable = true)
    private Long complete;

    @Schema(name = "最低流程拒绝数", nullable = true)
    private Long rejected;

    @Schema(name = "最低流程终止数", nullable = true)
    private Long terminated;

    @Schema(name = "最低流程总数", nullable = true)
    private Long total;
}
