package org.nf.neoflow.dto.version;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.nf.neoflow.constants.TimeFormat;

import java.time.LocalDateTime;

/**
 * 版本列表
 * @author PC8650
 */
public record VersionListDto(
        @Schema(name = "流程名称")
        String processName,

        @Schema(name = "版本号")
        Integer version,

        @Schema(name = "迭代自版本号")
        Integer iterateFrom,

        @Schema(name = "允许退回发起的次数")
        Integer cycle,

        @Schema(name = "终止方法")
        String terminateMethod,

        @Schema(name = "创建人标识")
        String createBy,

        @Schema(name = "创建人名称")
        String createByName,

        @Schema(name = "创建时间")
        @JsonFormat(pattern = TimeFormat.DATE_TIME)
        LocalDateTime createTime
){
}
