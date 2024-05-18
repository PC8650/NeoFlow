package org.nf.neoflow.dto.execute;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;
import org.nf.neoflow.constants.InstanceOperationType;
import org.nf.neoflow.dto.user.UserBaseInfo;
import org.nf.neoflow.exception.NeoExecuteException;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 执行表单
 * 在执行流程的过程中传递参数
 * @author PC8650
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ExecuteForm {

    public ExecuteForm(GraftForm form) {
        this.processName = form.getProcessName();
        this.version = form.getVersion();
        this.businessKey = form.getBusinessKey();
        this.num = form.getNum();
        this.nodeId = form.getNodeId();
        this.operationRemark = form.getOperationRemark();
        this.operationType = InstanceOperationType.PASS;
    }

    @Schema(name = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String processName;

    @Schema(name = "操作类型", description = "1-发起，2-通过，3-拒绝，4-转发，5-终止")
    @NotNull(message = "操作类型不能为空")
    @Range(min = 1, max = 5, message = "操作类型只能为1-发起，2-通过，3-拒绝，4-转发，5-终止")
    private Integer operationType;

    @Schema(name = "实例节点位置")
    private Integer num;

    @Schema(name = "节点id")
    private Long nodeId;

    @Schema(name = "流程实例业务key")
    private String businessKey;

    @Schema(name = "版本")
    private Integer version;

    @Schema(name = "操作方法")
    private String operationMethod;

    @Schema(name = "跳转条件")
    private Integer condition;

    @Schema(name = "操作用户信息")
    private UserBaseInfo operator;

    @Schema(name = "业务参数")
    private Map<String, Object> params;

    @Schema(name = "转发类型", description = "可选范围[模型节点操作类型]")
    private Integer forwardOperationType;

    @Schema(name = "转发对象")
    private List<UserBaseInfo> forwardOperator;

    @Schema(name = "操作备注")
    private String operationRemark;

    public void baseCheck() {
        if (num == null || num < 2) {
            throw new NeoExecuteException("实例节点位置应 >= 2");
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

    public void forwardCheck() {
        baseCheck();
        if (forwardOperationType == null) {
            throw new NeoExecuteException("转发类型不能为空");
        }
        if (CollectionUtils.isEmpty(forwardOperator)) {
            throw new NeoExecuteException("转发对象不能为空");
        }
        for (UserBaseInfo userBaseInfo : forwardOperator) {
            if (StringUtils.isBlank(userBaseInfo.getId()) || StringUtils.isBlank(userBaseInfo.getName())) {
                throw new NeoExecuteException("转发对象参数缺失");
            }
        }
    }

}
