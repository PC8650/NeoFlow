package org.nf.neoflow.dto.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nf.neoflow.constants.TimeFormat;
import org.nf.neoflow.dto.user.UserBaseInfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程实例操作历史
 * @author PC8650
 */
@Data
public class OperationHistoryDto {

        public OperationHistoryDto(){}

        public OperationHistoryDto(List<OperationHistoryDto.ListDto> history) {
                if (history == null) {
                        history = new ArrayList<>(0);
                }
                this.history = history;
        }

        @Schema(name = "操作历史")
        private List<ListDto> history;

        @Schema(name = "节点变量数据")
        private String variableData;

        public record ListDto(
                @JsonIgnore
                @Schema(name = "节点id")
                Long nodeId,

                @Schema(name = "节点位置")
                Integer num,

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
        ){}

}