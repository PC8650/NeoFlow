package com.nf.neoflow.dto.process;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nf.neoflow.constants.TimeFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程启用历史
 */
@Data
@AllArgsConstructor
public class ActiveVersionHistoryDto {

    @ApiModelProperty("启用版本号")
    private Integer version;

    @ApiModelProperty("启用人标识")
    private String id;

    @ApiModelProperty("启用人名称")
    private String name;

    @ApiModelProperty("启用时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime time;
}
