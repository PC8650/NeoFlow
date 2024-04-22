package com.nf.neoflow.dto.execute;

import com.nf.neoflow.dto.user.UserBaseInfo;
import com.nf.neoflow.exception.NeoExecuteException;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;

import java.util.Map;

/**
 * 执行表单
 * 在执行流程的过程中传递参数
 * @author PC8650
 */
@Data
@Accessors(chain = true)
public class ExecuteForm {

    @ApiModelProperty("流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String processName;

    @ApiModelProperty("操作类型：1-发起，2-通过，3-拒绝，4-转发，5-终止")
    @NotNull(message = "操作类型不能为空")
    @Range(min = 1, max = 5, message = "操作类型只能为1-发起，2-通过，3-拒绝，4-转发，5-终止")
    private Integer operationType;

    @ApiModelProperty("实例节点位置")
    private Integer num;

    @ApiModelProperty("节点id")
    private Long nodeId;

    @ApiModelProperty("流程实例业务key")
    private String businessKey;

    @ApiModelProperty("版本")
    private Integer version;

    @ApiModelProperty("操作方法")
    private String operationMethod;

    @ApiModelProperty("跳转条件")
    private Integer condition;

    @ApiModelProperty("操作用户信息")
    private UserBaseInfo operator;

    @ApiModelProperty("业务参数")
    private Map<String, Object> params;

    public void baseCheck() {
        if (num == null || num < 1) {
            throw new NeoExecuteException("实例节点位置应 >= 1");
        }
        if (nodeId == null || nodeId < 1) {
            throw new NeoExecuteException("实例节点id应 >= 1");
        }
        if (version == null || version < 1) {
            throw new NeoExecuteException("版本号应 >= 1");
        }
        if (StringUtils.isBlank(businessKey)) {
            throw new NeoExecuteException("业务key不能为空");
        }
    }

}
