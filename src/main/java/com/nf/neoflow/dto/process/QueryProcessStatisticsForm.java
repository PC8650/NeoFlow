package com.nf.neoflow.dto.process;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nf.neoflow.constants.TimeFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 流程统计查询表单
 */
@Data
public class QueryProcessStatisticsForm {

    @ApiModelProperty("流程名称")
    private String name;

    @ApiModelProperty("流程版本")
    private Integer version;

    @ApiModelProperty("流程开始时间起始")
    @JsonFormat(pattern = TimeFormat.DATE)
    private LocalDate beginStart;

    @ApiModelProperty("流程开始时间结束")
    @JsonFormat(pattern = TimeFormat.DATE)
    private LocalDate beginEnd;

    @ApiModelProperty("流程结束时间起始")
    @JsonFormat(pattern = TimeFormat.DATE)
    private LocalDate endStart;

    @ApiModelProperty("流程结束时间结束")
    @JsonFormat(pattern = TimeFormat.DATE)
    private LocalDate endEnd;

    @ApiModelProperty("最低流程进行数")
    private Long pending;

    @ApiModelProperty("最低流程完成数")
    private Long complete;

    @ApiModelProperty("最低流程拒绝数")
    private Long rejected;

    @ApiModelProperty("最低流程终止数")
    private Long terminated;

    @ApiModelProperty("最低流程总数")
    private Long total;
}
