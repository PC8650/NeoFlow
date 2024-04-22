package com.nf.neoflow.dto.version;

import com.nf.neoflow.exception.NeoProcessException;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 流程节点边
 * @author PC8650
 */
@Data
public class ProcessNodeEdge {

    @ApiModelProperty("开始节点标识")
    String startNode;

    @ApiModelProperty("结束节点标识")
    String endNode;

    @ApiModelProperty("条件")
    Integer condition;

    @ApiModelProperty("起始坐标")
    List<Double> startLocation;

    @ApiModelProperty("结束坐标")
    List<Double> endLocation;

    public void check() {
        if (StringUtils.isBlank(startNode) || StringUtils.isBlank(endNode)) {
            throw new NeoProcessException("边端点不能为空");
        }
        if (condition == null) {
            throw new NeoProcessException("边条件不能为空");
        }
        if (CollectionUtils.isEmpty(startLocation) || CollectionUtils.isEmpty(endLocation)) {
            throw new NeoProcessException("边坐标不能为空");
        }
    }

}
