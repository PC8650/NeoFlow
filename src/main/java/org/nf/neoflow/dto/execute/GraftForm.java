package org.nf.neoflow.dto.execute;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.nf.neoflow.dto.user.UserBaseInfo;
import org.nf.neoflow.exception.NeoExecuteException;

import java.util.Objects;

/**
 * 实例版本移植表单
 * @author PC8650
 */
@Data
public class GraftForm {

    @Schema(name = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String processName;

    @Schema(name = "版本",minimum = "1")
    @Min(value = 1, message = "版本号不能小于1")
    @NotNull(message = "版本不能为空")
    private Integer version;

    @Schema(name = "实例节点位置", minimum = "2")
    @Min(value = 2, message = "节点位置不能小于2")
    @NotNull(message = "实例节点位置不能为空")
    private Integer num;

    @Schema(name = "节点id")
    @Min(value = 1, message = "节点id不能小于1")
    @NotNull(message = "节点id不能为空")
    private Long nodeId;

    @Schema(description = "业务key")
    @NotBlank(message = "业务key不能为空")
    private String businessKey;

    @Schema(description = "移植版本",minimum = "1")
    @NotNull(message = "移植版本不能为空")
    @Min(value = 1, message = "版本号不能小于1")
    private Integer graftVersion;

    @Schema(description = "移植节点位置", nullable = true, minimum = "2")
    @Min(value = 2, message = "节点位置不能小于2")
    private Integer graftNum;

    @Schema(description = "是否执行当前节点方法")
    @NotNull(message = "请选择是否执行当前节点方法")
    private Boolean executeMethod;

    @Schema(description = "操作备注", nullable = true)
    private String operationRemark;

    @Schema(name = "操作用户信息")
    private UserBaseInfo operator;

    public void check(){
        if (version.equals(graftVersion)) {
            throw new NeoExecuteException("不能移植到同一个版本");
        }
    }

    public void increment(Long nodeId) {
        this.nodeId = nodeId;
        this.num += 1;
    }

}
