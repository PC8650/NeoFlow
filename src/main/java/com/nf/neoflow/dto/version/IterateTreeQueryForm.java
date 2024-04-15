package com.nf.neoflow.dto.version;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * 迭代树查询表单
 * @author PC8650
 */
@Data
public class IterateTreeQueryForm {

    @ApiModelProperty("流程名称")
    @NotBlank(message = "流程名称不能为空")
    private String processName;

    @ApiModelProperty("""
            查询类型，默认为1：
            1-非嵌套，迭代列表只包含直系迭代；
            2-嵌套，迭代列表包含非直系迭代；
            3-图，返回节点和关系
            """)
    @Range(min = 1, max = 3, message = "查询类型取值范围：1-非嵌套，2-嵌套，3-图")
    private Integer type = 1;
}
