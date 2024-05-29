package org.nf.neoflow.dto.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.nf.neoflow.constants.TimeFormat;

import java.time.LocalDateTime;

/**
 * 节点身份查询表单
 * @author PC8650
 */
public record QueryOfNodeIdentityDto(
    @Schema(name = "流程名称")
    String name,

    @Schema(name = "流程版本")
    Integer version,

    @Schema(name = "业务key")
    String businessKey,

    @Schema(name = "查询节点位置")
    Integer num,

    @Schema(name = "查询节点id")
    Long nodeId,

    @Schema(name = "查询节点状态", description = "1-待办，2-同意，3-拒绝，4-转发，5-终止")
    Integer status,

    @Schema(name = "结束时间", description = """
        待办：上一节点结束时间;
        已办：查询节点结束时间
    """)
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    LocalDateTime endTime,

    @Schema(name = "序列化的流程列表业务数据")
    String listData
) {
}
