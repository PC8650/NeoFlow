package com.nf.neoflow.dto.process;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nf.neoflow.constants.TimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程启用历史
 */
@Data
@AllArgsConstructor
public class ActiveVersionHistoryDto {

    @Schema(name = "启用版本号")
    private Integer version;

    @Schema(name = "启用人标识")
    private String activeId;

    @Schema(name = "启用人名称")
    private String activeName;

    @Schema(name = "启用时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime  activeTime;
}
