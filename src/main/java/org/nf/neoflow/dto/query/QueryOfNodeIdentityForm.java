package org.nf.neoflow.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * 节点身份查询表单
 * @author PC8650
 */
@Data
public class QueryOfNodeIdentityForm {

    @Schema(name = "查询类型", description = "1-待办，2-已办")
    @NotNull(message = "查询类型不能为空")
    @Range(min = 1, max = 2, message = "查询类型只能为1-待办，2-已办")
    private Integer queryType;

    @Schema(name = "已办节点状态", nullable = true, description = "2-同意，3-拒绝，4-转发，5-终止")
    @Range(min = 2, max = 5, message = "已办节点状态只能为2-同意，3-拒绝，4-转发，5-终止")
    private Integer nodeStatus;

    @Schema(name = "流程名称", nullable = true)
    private String name;

    @Schema(name = "流程版本", nullable = true, minimum = "1")
    @Min(value = 1, message = "版本号不能小于1")
    private Integer version;

    @Schema(name = "业务key", nullable = true)
    private String businessKey;

    @Schema(name = "节点名称", nullable = true)
    private String nodeName;

    @Schema(name = "节点身份", nullable = true)
    private String nodeIdentity;

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
