package org.nf.neoflow.dto.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nf.neoflow.constants.TimeFormat;
import org.nf.neoflow.dto.user.UserBaseInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 流程实例操作历史
 * @author PC8650
 */
@Data
public class OperationHistoryDto {

    @Schema(name = "节点名称")
    private String nodeName;

    @Schema(name = "节点候选人")
    private List<UserBaseInfo> candidate;

    @Schema(name = "节点操作人")
    private UserBaseInfo operator;

    @Schema(name = "操作结果", description = "1-待办，2-同意，3-拒绝，4-转发，5-终止")
    private Integer operationResult;

    @Schema(name = "版本移植")
    private String graft;

    @Schema(name = "操作备注")
    private String operationRemark;

    @Schema(name = "节点开始时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime beginTime;

    @Schema(name = "节点结束时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime endTime;

    @Schema(name = "节点持续时间")
    private String during;

    @Schema(name = "流程持续时间")
    private String processDuring;
}
