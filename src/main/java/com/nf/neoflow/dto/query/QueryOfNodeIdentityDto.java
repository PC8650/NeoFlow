package com.nf.neoflow.dto.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nf.neoflow.constants.TimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点身份查询表单
 * @author PC8650
 */
@Data
public class QueryOfNodeIdentityDto {

    @Schema(name = "流程名称")
    private String name;

    @Schema(name = "流程版本")
    private Integer version;

    @Schema(name = "业务key")
    private String businessKey;

    @Schema(name = "查询节点位置")
    private Integer num;

    @Schema(name = "查询节点id")
    private Long nodeId;

    @Schema(name = "查询节点状态", description = "1-待办，2-同意，3-拒绝，4-转发，5-终止")
    private Integer status;

    @Schema(name = "结束时间", description = """
        待办：上一节点结束时间;
        已办：查询节点结束时间
    """)
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime endTime;

}
