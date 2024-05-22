package org.nf.neoflow.dto.process;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.nf.neoflow.constants.TimeFormat;

import java.time.LocalDateTime;

/**
 * 流程启用历史
 * @author PC8650
 */
public record ActiveVersionHistoryDto(
        @Schema(name = "启用版本号")
        Integer version,

        @Schema(name = "启用人标识")
        String activeId,

        @Schema(name = "启用人名称")
        String activeName,

        @Schema(name = "启用时间")
        @JsonFormat(pattern = TimeFormat.DATE_TIME)
        LocalDateTime  activeTime
) {
}
