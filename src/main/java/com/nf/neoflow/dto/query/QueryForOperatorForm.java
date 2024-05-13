package com.nf.neoflow.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * 操作人流程查询表单
 * @author PC8650
 */
@Data
public class QueryForOperatorForm {

    @Schema(name = "查询类型", description = "1-发起，2-待办，3-已办")
    @NotNull(message = "查询类型不能为空")
    @Range(min = 1, max = 3, message = "查询类型只能为1-发起，2-待办，3-已办")
    private Integer type;

    @Schema(name = "已办列表-已办节点状态", nullable = true, description = "2-同意，3-拒绝，4-转发，5-终止")
    @Range(min = 2, max = 5, message = "已办节点状态只能为2-同意，3-拒绝，4-转发，5-终止")
    private Integer nodeStatus;

    @Schema(name = "流程实例当前状态", nullable = true, description = "1-进行中，2-通过，3-未通过，4-强行终止")
    @Range(min = 1, max = 4, message = "流程实例当前状态只能为1-进行中，2-通过，3-未通过，4-强行终止")
    private Integer InstanceStatus;

    @Schema(name = "流程名称", nullable = true)
    private String name;

    @Schema(name = "流程版本", nullable = true)
    private Integer version;

    @Schema(name = "业务key", nullable = true)
    private String businessKey;

    @Schema(name = "当前用户id", nullable = true, description = "Neo4jConfig.baseUserChoose的配置选择")
    private String userId;

    @Schema(name = "当前用户名称", nullable = true, description = "Neo4jConfig.baseUserChoose的配置选择")
    private String username;

    @Schema(name = "是否降序", nullable = true, defaultValue = "true")
    private Boolean desc = true;

    @Schema(name = "页码", nullable = true, defaultValue = "1")
    private Integer pageNumber = 1;

    @Schema(name = "每页显示数量", nullable = true, defaultValue = "15")
    private Integer pageSize = 15;

}
