package com.nf.neoflow.dto.query;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * 操作人流程查询表单
 * @author PC8650
 */
@Data
public class QueryForOperatorForm {

    @ApiModelProperty("查询类型：1-发起，2-待办，3-已办")
    @NotNull(message = "查询类型不能为空")
    @Range(min = 1, max = 3, message = "查询类型只能为1-发起，2-待办，3-已办")
    private Integer type;

    @ApiModelProperty("已办列表-已办节点状态")
    @Range(min = 2, max = 5, message = "已办节点状态只能为2-同意，3-拒绝，4-转发，5-终止")
    private Integer nodeStatus;

    @ApiModelProperty("流程实例当前状态")
    @Range(min = 1, max = 4, message = "流程实例当前状态只能为1-进行中，2-通过，3-未通过，4-强行终止")
    private Integer InstanceStatus;

    @ApiModelProperty("流程名称")
    private String name;

    @ApiModelProperty("流程版本")
    private Integer version;

    @ApiModelProperty("业务key")
    private String businessKey;

    @ApiModelProperty("当前用户id")
    private String userId;

    @ApiModelProperty("当前用户名称")
    private String username;

    @ApiModelProperty("是否降序")
    private Boolean desc = true;

    @ApiModelProperty("页码")
    private Integer pageNumber = 1;

    @ApiModelProperty("每页显示数量")
    private Integer pageSize = 15;

}
