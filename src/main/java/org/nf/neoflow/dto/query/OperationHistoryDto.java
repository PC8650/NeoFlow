package org.nf.neoflow.dto.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.nf.neoflow.constants.TimeFormat;
import org.nf.neoflow.dto.user.UserBaseInfo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程实例操作历史
 * @author PC8650
 */
public record OperationHistoryDto(
        @Schema(name = "节点名称")
        String nodeName,

        @Schema(name = "节点候选人")
        List<UserBaseInfo> candidate,

        @Schema(name = "节点操作人")
        UserBaseInfo operator,

        @Schema(name = "操作结果", description = "1-待办，2-同意，3-拒绝，4-转发，5-终止")
        Integer operationResult,

        @Schema(name = "版本移植")
        String graft,

        @Schema(name = "操作备注")
        String operationRemark,

        @Schema(name = "节点开始时间")
        @JsonFormat(pattern = TimeFormat.DATE_TIME)
        LocalDateTime beginTime,

        @Schema(name = "节点结束时间")
        @JsonFormat(pattern = TimeFormat.DATE_TIME)
        LocalDateTime endTime,

        @Schema(name = "节点持续时间")
        String during,

        @Schema(name = "流程持续时间")
        String processDuring
) {
}