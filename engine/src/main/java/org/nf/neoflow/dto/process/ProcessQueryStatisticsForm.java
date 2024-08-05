package org.nf.neoflow.dto.process;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import org.nf.neoflow.constants.TimeFormat;

import java.time.LocalDate;

/**
 * 流程统计查询表单
 */
public record ProcessQueryStatisticsForm(
        @Schema(name = "流程名称", nullable = true)
        String name,

        @Schema(name = "流程版本", nullable = true, minimum = "1")
        @Min(value = 1, message = "版本号不能小于1")
        Integer version,

        @Schema(name = "流程开始时间起始", nullable = true)
        @JsonFormat(pattern = TimeFormat.DATE)
        LocalDate beginStart,

        @Schema(name = "流程开始时间结束", nullable = true)
        @JsonFormat(pattern = TimeFormat.DATE)
        LocalDate beginEnd,

        @Schema(name = "流程结束时间起始", nullable = true)
        @JsonFormat(pattern = TimeFormat.DATE)
        LocalDate endStart,

        @Schema(name = "流程结束时间结束", nullable = true)
        @JsonFormat(pattern = TimeFormat.DATE)
        LocalDate endEnd,

        @Schema(name = "最低流程进行数", nullable = true, minimum = "0")
        @Min(value = 0, message = "流程进行数不能小于0")
        Long pending,

        @Schema(name = "最低流程完成数", nullable = true, minimum = "0")
        @Min(value = 0, message = "流程完成数不能小于0")
        Long complete,

        @Schema(name = "最低流程拒绝数", nullable = true, minimum = "0")
        @Min(value = 0, message = "流程拒绝数不能小于0")
        Long rejected,

        @Schema(name = "最低流程终止数", nullable = true, minimum = "0")
        @Min(value = 0, message = "流程终止数不能小于0")
        Long terminated,

        @Schema(name = "最低流程总数", nullable = true, minimum = "0")
        @Min(value = 0, message = "流程总数不能小于0")
        Long total
) {
}
