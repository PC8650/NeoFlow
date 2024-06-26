package org.nf.neoflow.dto.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nf.neoflow.constants.TimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作人流程查询
 * @author PC8650
 */
@Data
public class QueryForOperatorDto {

    @Schema(name = "流程名称")
    private String name;

    @Schema(name = "激活版本")
    private Integer activeVersion;

    @Schema(name = "当前实例版本")
    private Integer version;

    @Schema(name = "业务key")
    private String businessKey;

    @Schema(name = "发起时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime initiateTime;

    @Schema(name = "实例更新时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime updateTime;

    @Schema(name = "当前实例节点长度")
    private Integer num;

    @Schema(name = "当前实例最后的节点id")
    private Long nodeId;

    @Schema(name = "状态", description = """
        发起列表，当前流程实例状态：1-进行中，2-通过，3-未通过，4-强行终止；
        已办列表：null；
        待办列表：null.
    """)
    private Integer status;

    @Schema(name = "已办列表-已办节点列表")
    private List<DoneNode> doneNodes;

    @Schema(name = "序列化的流程列表业务数据")
    private String listData;


    /**
     * 已办节点
     */
    private record DoneNode(
            @Schema(name = "已办节点位置")
            Integer num,

            @Schema(name = "已办节点id")
            Long nodeId,

            @Schema(name = "已办节点名称")
            String nodeName,

            @Schema(name = "已办节点标识")
            String nodeIdentity,

            @Schema(name = "已办节点状态", description = "2-同意，3-拒绝，4-转发，5-终止")
            Integer status,

            @Schema(name = "版本移植")
            String graft,

            @Schema(name = "已办节点处理时间")
            @JsonFormat(pattern = TimeFormat.DATE_TIME)
            LocalDateTime doneTime
    ){}

}
