package com.nf.neoflow.dto.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nf.neoflow.constants.TimeFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作人流程查询
 * @author PC8650
 */
@Data
public class QueryForOperatorDto {

    @ApiModelProperty("流程名称")
    private String name;

    @ApiModelProperty("流程版本")
    private Integer version;

    @ApiModelProperty("业务key")
    private String businessKey;

    @ApiModelProperty("发起时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime initiateTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime updateTime;

    @ApiModelProperty("当前实例节点长度")
    private Integer num;

    @ApiModelProperty("当前实例节点id")
    private Long nodeId;

    @ApiModelProperty("""
        发起列表，当前流程实例状态：1-进行中，2-通过，3-未通过，4-强行终止；
        已办列表，null；
        待办列表：null.
    """)
    private Integer status;

    @ApiModelProperty("已办节点列表")
    List<DoneNode> doneNodes;


    /**
     * 已办节点
     * @param num 已办节点长度
     * @param nodeId 已办节点id
     * @param nodeName 已办节点名称
     * @param status 已办节点状态
     * @param doneTime 已办节点处理时间
     */
    private record DoneNode(
            @ApiModelProperty("已办节点长度")
            Integer num,
            @ApiModelProperty("已办节点id")
            Long nodeId,
            @ApiModelProperty("已办节点名称")
            String nodeName,
            @ApiModelProperty("已办节点状态")
            Integer status,
            @ApiModelProperty("已办节点处理时间")
            @JsonFormat(pattern = TimeFormat.DATE_TIME)
            LocalDateTime doneTime
    ){}

}
